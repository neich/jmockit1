/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.primepaths;

import mockit.external.asm.Label;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public final class PPNodeBuilder
{
   public int firstLine;
   @Nonnull final List<PPNode> nodes = new ArrayList<PPNode>();

   @Nullable private PPNode entryNode;
   @Nullable private Map<Class<?>, Label> catch2label = new HashMap<>();
   @Nullable private Map<Label, Class<?>> label2catch = new HashMap<>();
   @Nonnull private final Map<Label, List<PPNode>> jumpTargetToNodes = new LinkedHashMap<Label, List<PPNode>>();
   @Nonnull private final Map<Label, List<PPNode>> gotoTargetToSuccessors = new LinkedHashMap<Label, List<PPNode>>();
   @Nonnull private final Map<Label, PPNode> labelToNode = new LinkedHashMap<Label, PPNode>();
   private boolean insideTryCatch = false;

   private int potentiallyTrivialJump;
   @Nullable private PPNode nodeExitException;
   @Nullable private PPNode currentNode;
   @Nullable private Label finallyClause;
   private boolean insideCatchOrFinally;

   private int addNewNode(@Nonnull PPNode newNode)
   {
      int newNodeIndex = nodes.size();

      nodes.add(newNode);

      if (newNodeIndex > 0) {
         PPNode precedingNode = nodes.get(newNodeIndex - 1);

         if (precedingNode.line == newNode.line) {
            newNode.setSegmentAccordingToPrecedingNode(precedingNode);
         }
      }

      return newNodeIndex;
   }

   public boolean hasNodes() { return !nodes.isEmpty(); }

   public void handleEntry(int line)
   {
      firstLine = line;
      entryNode = new PPNode.Entry(line);

      currentNode = entryNode;
      addNewNode(entryNode);
   }

   public int handleExit(int exitLine)
   {
      PPNode newNode = new PPNode.Exit(exitLine);
      connectNode(newNode);

      currentNode = newNode;
      return addNewNode(newNode);
   }


   public int handleRegularInstruction(int line, int opcode)
   {
      if (currentNode.isRegular()) return -1;

      PPNode newNode = new PPNode.BasicBlock(line);
      newNode.setSubsumable(true);
      connectNode(newNode);

      currentNode = newNode;
      return addNewNode(newNode);
   }

   public int handleMethodCall(int line, Class<?>[] exceptions) {

      if (exceptions.length == 0 && finallyClause == null) return handleRegularInstruction(line, -1);

      PPNode newFork = new PPNode.Fork(line);
      connectNode(newFork);
      int nodeIndex = addNewNode(newFork);

      currentNode = newFork;

      if (exceptions.length > 0) {
         boolean linkedToFinally = false;
         for (Class<?> c : exceptions) {
            if (catch2label.containsKey(c)) {
               connectNodeToLabel(catch2label.get(c), newFork);
            } else {
               if (finallyClause != null && !linkedToFinally) {
                  connectNodeToLabel(finallyClause, newFork);
                  linkedToFinally = true;
               } else {
                  PPNode exitException = new PPNode.Exit(line);
                  connectNodeToNode(newFork, exitException);
                  addNewNode(exitException);
               }
            }
         }
      } else {
         connectNodeToLabel(finallyClause, newFork);
      }

      return nodeIndex;
   }

   public int handleJump(@Nonnull Label targetBlock, int line, boolean conditional)
   {
      PPNode n = conditional ? new PPNode.Fork(line) : new PPNode.Goto(line);

      connectNodeToLabel(targetBlock, n);
      connectNode(n);

      currentNode = n;
      return addNewNode(n);
   }

   public int handleMultipleJump(@Nonnull Label[] targets, int line)
   {
      PPNode newFork = new PPNode.Fork(line);
      connectNode(newFork);
         // assert currentSimpleFork == null;
      for (Label l : targets)
         connectNodeToLabel(l, newFork);
      potentiallyTrivialJump = 1;
      return addNewNode(newFork);
   }

   public int handleJumpTarget(@Nonnull Label jumpTarget, int line)
   {
      if (entryNode == null) return -1;

      if (label2catch.containsKey(jumpTarget)) {
         insideCatchOrFinally = true;
         Class<?> clazz = label2catch.get(jumpTarget);
         label2catch.remove(jumpTarget);
         catch2label.remove(clazz);
         if (label2catch.size() == 0) {
            insideTryCatch = false;
            finallyClause = null;
         }
      } else if (jumpTarget == finallyClause) {
         finallyClause = null;
         insideCatchOrFinally = true;
      }
      else
         insideCatchOrFinally = false;

      PPNode newNode = new PPNode.Join(line);
      if (!insideTryCatch)
         newNode.setSubsumable(true);
      labelToNode.put(jumpTarget, newNode);
      connectNodesToTargetedJoin(jumpTarget, newNode);
      connectNode(newNode);

      currentNode = newNode;
      return addNewNode(newNode);
   }

   public int handleTryCatch(int line, Label start, Label end, Label handler, String type) {
      if (type != null) {
         insideTryCatch = true;
         try {
            Class<?> eclazz = Class.forName(type.replace('/', '.'), false, this.getClass().getClassLoader());
            catch2label.put(eclazz, handler);
            label2catch.put(handler, eclazz);
         } catch (ClassNotFoundException e) {
         }
      } else {
         finallyClause = handler;
      }

      return -1;
   }

   private boolean isNewLineTarget(@Nonnull Label basicBlock)
   {
      return !jumpTargetToNodes.containsKey(basicBlock) && !gotoTargetToSuccessors.containsKey(basicBlock);
   }

   private void connectNode(@Nonnull PPNode node)
   {
      if (!currentNode.isGoto() && !(currentNode instanceof PPNode.Exit)) {
         currentNode.setNextConsecutiveNode(node);
      }
   }

   private void connectNodeToLabel(@Nonnull Label targetBlock, @Nonnull PPNode node)
   {
      PPNode targetNode = labelToNode.get(targetBlock);

       if (targetNode != null) {
            node.addSuccessor(targetNode);
         }
       else
          setUpMappingFromConditionalTargetToNode(targetBlock, node);
   }

   private void connectNodeToNode(@Nonnull PPNode fromNode, @Nonnull PPNode toNode)
   {
      fromNode.setNextConsecutiveNode(toNode);
   }

   private void setUpMappingFromConditionalTargetToNode(@Nonnull Label targetBlock, @Nonnull PPNode node)
   {
      if (labelToNode.containsKey(targetBlock)) return;

      List<PPNode> nodesWithSameTarget = jumpTargetToNodes.get(targetBlock);

      if (nodesWithSameTarget == null) {
         nodesWithSameTarget = new LinkedList<PPNode>();
         jumpTargetToNodes.put(targetBlock, nodesWithSameTarget);
      }

      nodesWithSameTarget.add(node);
   }

   private void connectNodesToTargetedJoin(@Nonnull Label target, @Nonnull PPNode newJoin)
   {
      List<PPNode> nodes = jumpTargetToNodes.get(target);

      if (nodes != null) {
         for (PPNode n : nodes) {
            n.addSuccessor(newJoin);
         }

         jumpTargetToNodes.remove(target);
      }
   }

   public int handleForwardJumpsToNewTargets(@Nonnull Label defaultBlock, @Nonnull Label[] caseBlocks, int line)
   {
      PPNode newJoin = new PPNode.Join(line);

      for (Label targetBlock : caseBlocks) {
         if (targetBlock != defaultBlock) {
            connectNodeToLabel(targetBlock, newJoin);
         }
      }

      connectNodeToLabel(defaultBlock, newJoin);

      return addNewNode(newJoin);
   }
}
