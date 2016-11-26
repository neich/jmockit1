/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.paths;

import mockit.coverage.paths.Node.*;
import mockit.external.asm.Label;
import mockit.external.asm.Opcodes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public final class NodeBuilder
{
   public int firstLine;
   @Nonnull final List<Node> nodes = new ArrayList<Node>();

   @Nullable private Entry entryNode;
   @Nullable private SimpleFork currentSimpleFork;
   @Nullable private BasicBlock currentBasicBlock;
   @Nullable private Join currentJoin;
   @Nullable private Map<Class<?>, Label> catch2label = new HashMap<>();
   @Nullable private Map<Label, Class<?>> label2catch = new HashMap<>();
   @Nonnull private final Map<Label, List<Fork>> jumpTargetToForks = new LinkedHashMap<Label, List<Fork>>();
   @Nonnull private final Map<Label, List<Goto>> gotoTargetToSuccessors =
      new LinkedHashMap<Label, List<Goto>>();
   @Nonnull private final Map<Label, Join> labelToJoin = new LinkedHashMap<Label, Join>();

   private int potentiallyTrivialJump;
   private Node nodeExitException = null;

   public void handleEntry(int line)
   {
      firstLine = line;
      entryNode = new Entry(line);
      addNewNode(entryNode);
   }

   private int addNewNode(@Nonnull Node newNode)
   {
      int newNodeIndex = nodes.size();

      if (newNodeIndex == 0 && !(newNode instanceof Entry)) {
         return -1;
      }

      nodes.add(newNode);

      if (newNodeIndex > 0) {
         Node precedingNode = nodes.get(newNodeIndex - 1);

         if (precedingNode.line == newNode.line) {
            newNode.setSegmentAccordingToPrecedingNode(precedingNode);
         }
      }

      return newNodeIndex;
   }

   public boolean hasNodes() { return !nodes.isEmpty(); }

   public int handleRegularInstruction(int line, int opcode)
   {
      if (currentSimpleFork == null) {
         potentiallyTrivialJump = 0;
         return -1;
      }

      assert currentBasicBlock == null;

      BasicBlock newNode = new BasicBlock(line);
      connectNodes(newNode, opcode);

      return addNewNode(newNode);
   }

   public int handleMethodCall(int line, Class<?>[] exceptions) {
      if (catch2label.size() == 0) {
         // Not inside a try catch block
         if (nodeExitException == null) {
            nodeExitException = new Node.Exit(-1);
            addNewNode(nodeExitException);
         }
         SimpleFork newFork = new SimpleFork(line);
         // assert currentSimpleFork == null;
         connectNodes(newFork, nodeExitException);
         currentSimpleFork = newFork;
         potentiallyTrivialJump = 1;
         return addNewNode(newFork);

      } else {
         for (Class<?> c : exceptions) {
            if (catch2label.containsKey(c)) {
               handleJump(catch2label.get(c), line, true);
            }
         }
      }

      return -1;
   }

   public int handleJump(@Nonnull Label targetBlock, int line, boolean conditional)
   {
      if (conditional) {
         SimpleFork newFork = new SimpleFork(line);
         // assert currentSimpleFork == null;
         connectNodes(targetBlock, newFork);
         currentSimpleFork = newFork;
         potentiallyTrivialJump = 1;
         return addNewNode(newFork);
      }
      else {
         Goto newGoto = new Goto(line);
         connectNodes(targetBlock, newGoto);
         return addNewNode(newGoto);
      }

   }

   public int handleJumpTarget(@Nonnull Label jumpTarget, int line)
   {
      // Ignore for visitLabel calls preceding visitLineNumber:
/*
      if (isNewLineTarget(basicBlock)) {
         return -1;
      }
*/


      if (label2catch.containsKey(jumpTarget)) {
         Class<?> clazz = label2catch.get(jumpTarget);
         label2catch.remove(jumpTarget);
         catch2label.remove(clazz);
      }


      Join newNode = new Join(line);
      labelToJoin.put(jumpTarget, newNode);
      connectNodes(jumpTarget, newNode);

      return addNewNode(newNode);
   }

   public int handleTryCatch(int line, Label start, Label end, Label handler, String type) {
      if (type != null) {
         try {
            Class<?> eclazz = Class.forName(type.replace('/', '.'), false, this.getClass().getClassLoader());
            catch2label.put(eclazz, handler);
            label2catch.put(handler, eclazz);
         } catch (ClassNotFoundException e) {
         }
      }

      return -1;
   }

   private boolean isNewLineTarget(@Nonnull Label basicBlock)
   {
      return !jumpTargetToForks.containsKey(basicBlock) && !gotoTargetToSuccessors.containsKey(basicBlock);
   }

   private void connectNodes(@Nonnull BasicBlock newBasicBlock, int opcode)
   {
      if (currentSimpleFork != null) {
         currentSimpleFork.setNextConsecutiveNode(newBasicBlock);
         currentSimpleFork = null;

         if (potentiallyTrivialJump == 1) {
            potentiallyTrivialJump = opcode == Opcodes.ICONST_1 ? 2 : 0;
         }
      }
      else {
         assert currentJoin != null;

         if (potentiallyTrivialJump == 3) {
            if (opcode == Opcodes.ICONST_0) {
               currentJoin.fromTrivialFork = true;
            }

            potentiallyTrivialJump = 0;
         }

         currentJoin.setNextConsecutiveNode(newBasicBlock);
         currentJoin = null;
      }

      currentBasicBlock = newBasicBlock;
   }

   private void connectNodes(@Nonnull Label targetBlock, @Nonnull Fork newFork)
   {
      assert entryNode != null;

      if (entryNode.getNextConsecutiveNode() == null) {
         entryNode.setNextConsecutiveNode(newFork);
      }

      Join join = labelToJoin.get(targetBlock);
      if (join != null) {
         newFork.addNextNode(join);
      }
      else
         setUpMappingFromConditionalTargetToFork(targetBlock, newFork);
      connectNodes(newFork);
   }

   private void connectNodes(SimpleFork newFork, Node nodeExitException) {
      newFork.addNextNode(nodeExitException);
      connectNodes(newFork);
   }

   private void connectNodes(@Nonnull Label targetBlock, @Nonnull Goto newGoto)
   {
      assert entryNode != null;

      if (entryNode.getNextConsecutiveNode() == null) {
         entryNode.setNextConsecutiveNode(newGoto);
      }

      Join join = labelToJoin.get(targetBlock);
      if (join != null) {
         newGoto.setNextNodeAfterGoto(join);
      }
      else
         setUpMappingFromGotoTargetToCurrentGotoSuccessor(targetBlock, newGoto);
      connectNodes(newGoto);
   }


   private void setUpMappingFromConditionalTargetToFork(@Nonnull Label targetBlock, @Nonnull Fork newFork)
   {
      if (labelToJoin.containsKey(targetBlock)) return;

      List<Fork> forksWithSameTarget = jumpTargetToForks.get(targetBlock);

      if (forksWithSameTarget == null) {
         forksWithSameTarget = new LinkedList<Fork>();
         jumpTargetToForks.put(targetBlock, forksWithSameTarget);
      }

      forksWithSameTarget.add(newFork);
   }

   private void setUpMappingFromGotoTargetToCurrentGotoSuccessor(@Nonnull Label targetBlock, @Nullable Goto gotoNode)
   {
      if (labelToJoin.containsKey(targetBlock)) return;

      List<Goto> successors = gotoTargetToSuccessors.get(targetBlock);

      if (successors == null) {
         successors = new LinkedList<Goto>();
         gotoTargetToSuccessors.put(targetBlock, successors);
      }

      successors.add(gotoNode);
   }

   private void connectNodes(@Nonnull Label basicBlock, @Nonnull Join newJoin)
   {
      connectNodes(newJoin);
      connectSourceForksToTargetedJoin(basicBlock, newJoin);
      connectGotoSuccessorsToNewJoin(basicBlock, newJoin);
      currentJoin = newJoin;
   }

   public int handleExit(int exitLine)
   {
      Exit newNode = new Exit(exitLine);
      connectNodes(newNode);

      return addNewNode(newNode);
   }

   private void connectNodes(@Nonnull Node newNode)
   {
      if (entryNode.getNextConsecutiveNode() == null) {
         entryNode.setNextConsecutiveNode(newNode);
         assert currentSimpleFork == null;
         assert currentJoin == null;
         assert currentBasicBlock == null;

      }

      if (currentSimpleFork != null) {
         currentSimpleFork.setNextConsecutiveNode(newNode);
         currentSimpleFork = null;
         assert currentJoin == null;
         assert currentBasicBlock == null;
      }

      if (currentJoin != null) {
         currentJoin.setNextConsecutiveNode(newNode);
         currentJoin = null;
         assert currentBasicBlock == null;
      }

      if (currentBasicBlock != null) {
         currentBasicBlock.setNextConsecutiveNode(newNode);
         currentBasicBlock = null;
      }
   }

   private void connectSourceForksToTargetedJoin(@Nonnull Label targetBlock, @Nonnull Join newJoin)
   {
      List<Fork> forks = jumpTargetToForks.get(targetBlock);

      if (forks != null) {
         for (Fork fork : forks) {
            fork.addNextNode(newJoin);
         }

         jumpTargetToForks.remove(targetBlock);
      }
   }

   private void connectGotoSuccessorsToNewJoin(@Nonnull Label targetBlock, @Nonnull Join newJoin)
   {
      List<Goto> successors = gotoTargetToSuccessors.get(targetBlock);

      if (successors != null) {
         for (Goto successorToGoto : successors) {
            successorToGoto.setNextNodeAfterGoto(newJoin);
         }

         gotoTargetToSuccessors.remove(targetBlock);
      }
   }

   public int handleForwardJumpsToNewTargets(@Nonnull Label defaultBlock, @Nonnull Label[] caseBlocks, int line)
   {
      Fork newJoin = new MultiFork(line);

      for (Label targetBlock : caseBlocks) {
         if (targetBlock != defaultBlock) {
            connectNodes(targetBlock, newJoin);
         }
      }

      connectNodes(defaultBlock, newJoin);

      return addNewNode(newJoin);
   }
}
