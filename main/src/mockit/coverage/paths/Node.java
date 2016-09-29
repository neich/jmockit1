/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.paths;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ClassReferencesSubclass")
public class Node implements Serializable
{
   private static final long serialVersionUID = 7521062699264845946L;

   @Nonnull private final transient ThreadLocal<Boolean> reached = new ThreadLocal<Boolean>();
   public final int line;
   protected int segment;
   protected List<Node> incomingNodes = new ArrayList<Node>();

   @Nullable protected Node nextConsecutiveNode;

   private Node(int line) {
      this.line = line;
   }

   @Nullable
   public Node getNextConsecutiveNode() {
      return nextConsecutiveNode;
   }

   public void setNextConsecutiveNode(@Nullable Node nextConsecutiveNode) {
      this.nextConsecutiveNode = nextConsecutiveNode;
      nextConsecutiveNode.addIncomingNode(this);
   }

   void setSegmentAccordingToPrecedingNode(@Nonnull Node precedingNode)
   {
      int currentSegment = precedingNode.segment;
      segment = precedingNode instanceof Fork ? currentSegment + 1 : currentSegment;
   }

   public final int getSegment() { return segment; }

   final void setReached(@Nullable Boolean reached) { this.reached.set(reached); }
   final boolean wasReached() { return reached.get() != null; }

   @Override
   public final String toString() { return getClass().getSimpleName() + ':' + line + '-' + segment; }

   public void addIncomingNode(Node node) {
      incomingNodes.add(node);
   }

   public List<Node> getIncomingNodes() { return this.incomingNodes; }

   static final class Entry extends Node
   {
      private static final long serialVersionUID = -3065417917872259568L;
      public List<Path> primePaths = new ArrayList<Path>();

      Entry(int entryLine) { super(entryLine); }
   }

   interface GotoSuccessor extends Serializable
   {
      void setNextNodeAfterGoto(@Nonnull Join newJoin);
   }

   static final class Exit extends Node
   {
      private static final long serialVersionUID = -4801498566218642509L;
      @Nonnull final List<Path> paths = new ArrayList<Path>(4);

      Exit(int exitLine) { super(exitLine); }
   }

   static final class BasicBlock extends Node
   {
      private static final long serialVersionUID = 2637678937923952603L;

      BasicBlock(int startingLine) { super(startingLine); }

   }

   public abstract static class Fork extends Node
   {
      private static final long serialVersionUID = -4995089238476806249L;

      Fork(int line) { super(line); }

      abstract void addNextNode(@Nonnull Join nextNode);
   }

   static final class SimpleFork extends Fork
   {
      private static final long serialVersionUID = -521666665272332763L;
      @Nullable private Join nextNodeAfterJump;

      SimpleFork(int line) { super(line); }

      @Override
      void addNextNode(@Nonnull Join nextNode) {
         nextNodeAfterJump = nextNode;
         nextNode.addIncomingNode(this);
      }

      Node getNextNodeAfterJump() { return this.nextNodeAfterJump; }
   }

   static final class MultiFork extends Fork
   {
      private static final long serialVersionUID = 1220318686622690670L;
      @Nonnull private final List<Join> caseNodes = new ArrayList<Join>();

      MultiFork(int line) { super(line); }

      @Override
      void addNextNode(@Nonnull Join nextNode) { caseNodes.add(nextNode); }

   }

   static final class Join extends Node
   {
      private static final long serialVersionUID = -1983522899831071765L;
      transient boolean fromTrivialFork;

      Join(int joiningLine) { super(joiningLine); }

/*
      @Override
      void setSegmentAccordingToPrecedingNode(@Nonnull Node precedingNode)
      {
         segment = precedingNode.segment + 1;
      }
*/
   }

   static final class Goto extends Node
   {
      private static final long serialVersionUID = -4715451134432419220L;
      private Join nextNodeAfterGoto;

      Goto(int line) { super(line); }

      public void setNextNodeAfterGoto(@Nonnull Join newJoin) {
         nextNodeAfterGoto = newJoin;
         newJoin.addIncomingNode(this);
      }

      public Node getNextNodeAfterGoto() { return nextNodeAfterGoto; }

//      @Override
//      public Node getNextConsecutiveNode() { return this.nextNodeAfterGoto; }
   }
}
