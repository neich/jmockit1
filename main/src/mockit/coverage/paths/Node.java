/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.paths;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ClassReferencesSubclass")
public class Node implements Serializable
{

   public static class LineSegment {
      public int line;
      public int segment;

      public LineSegment(int l, int s) {
         this.line = l;
         this.segment = s;
      }
   }

   private static final long serialVersionUID = 7521062699264845946L;

   @Nonnull private final transient ThreadLocal<Boolean> reached = new ThreadLocal<Boolean>();
   public final int line;
   protected int segment;
   protected List<LineSegment> extraLineSegments = new ArrayList<>();
   protected List<Node> incomingNodes = new ArrayList<Node>();
   protected boolean isSimplified = false;

   @Nullable protected Node nextConsecutiveNode;

   private Node(int line) {
      this.line = line;
   }

   public boolean isSimplified() {
      return isSimplified;
   }

   public void setSimplified() { this.isSimplified = true; }

   @Nullable
   public Node getNextConsecutiveNode() {
      return nextConsecutiveNode;
   }

   public void swapIncomingNode(Node oldNode, Node newNode) {
      incomingNodes.set(incomingNodes.indexOf(oldNode), newNode);
   }

   public void setNextConsecutiveNode(@Nullable Node nextConsecutiveNode) {
      this.nextConsecutiveNode = nextConsecutiveNode;
      nextConsecutiveNode.addIncomingNode(this);
   }

   public void swapNextConsecutiveNode(Node newNode) {

      if (nextConsecutiveNode != null) {
         nextConsecutiveNode.removeIncomingNode(this);
         if (newNode != null) {
            newNode.removeIncomingNode(nextConsecutiveNode);
            newNode.addIncomingNode(this);
         }
         this.extraLineSegments.add(new LineSegment(nextConsecutiveNode.line, nextConsecutiveNode.segment));

      }
      nextConsecutiveNode = newNode;
   }

   public boolean fuse(Node n) {
      if (n instanceof Fork || (n instanceof Exit && !(this instanceof Entry))) {
         n.moveIncomingNodes(this);
         n.extraLineSegments.addAll(this.getExtraLineSegments());
         return false;
      } else {
         this.extraLineSegments.addAll(n.getExtraLineSegments());
         this.swapNextConsecutiveNode(n.getNextConsecutiveNode());
         return true;
      }
   }

   private void moveIncomingNodes(Node node) {
      this.incomingNodes.clear();
      for (Node n: node.getIncomingNodes()) {
         this.addIncomingNode(n);
         n.replaceNextNode(node, this);
      }
   }

   public void replaceNextNode(Node oldNode, Node newNode) {
      if (nextConsecutiveNode == oldNode) nextConsecutiveNode = newNode;
   }


   private void removeIncomingNode(Node node) {
      incomingNodes.remove(node);
   }

   void setSegmentAccordingToPrecedingNode(@Nonnull Node precedingNode)
   {
      int currentSegment = precedingNode.segment;
      segment = precedingNode instanceof Fork ? currentSegment + 1 : currentSegment;
   }

   public final int getSegment() { return segment; }

   public List<LineSegment> getExtraLineSegments() { return this.extraLineSegments; }

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

      abstract void addNextNode(@Nonnull Node nextNode);
   }

   public static final class SimpleFork extends Fork
   {
      private static final long serialVersionUID = -521666665272332763L;
      @Nullable private Node nextNodeAfterJump;
      protected List<LineSegment> extraLineSegmentsForJump = new ArrayList<>();

      SimpleFork(int line) { super(line); }

      @Override
      void addNextNode(@Nonnull Node nextNode) {
         nextNodeAfterJump = nextNode;
         nextNode.addIncomingNode(this);
      }

      @Override
      public void replaceNextNode(Node oldNode, Node newNode) {
         if (nextConsecutiveNode == oldNode) nextConsecutiveNode = newNode;
         else if (nextNodeAfterJump == oldNode) nextNodeAfterJump = newNode;
      }

      void swapNextNodeAfterJump(@Nonnull Node nextNode) {
         if (nextNodeAfterJump != null) {
            nextNodeAfterJump.removeIncomingNode(this);
            nextNode.removeIncomingNode(nextNodeAfterJump);
            nextNode.addIncomingNode(this);
            this.extraLineSegmentsForJump.add(new LineSegment(nextNodeAfterJump.line, nextNodeAfterJump.segment));
         }
         nextNodeAfterJump = nextNode;
      }

      public Node getNextNodeAfterJump() { return this.nextNodeAfterJump; }

      public List<LineSegment> getExtraLineSegmentsForJump() { return this.extraLineSegmentsForJump; }
   }

   static final class MultiFork extends Fork
   {
      private static final long serialVersionUID = 1220318686622690670L;
      @Nonnull private final List<Node> caseNodes = new ArrayList<Node>();

      MultiFork(int line) { super(line); }

      @Override
      void addNextNode(@Nonnull Node nextNode) { caseNodes.add(nextNode); }

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

      Goto(int line) { super(line); }

      public void setNextNodeAfterGoto(@Nonnull Join newJoin) {
         setNextConsecutiveNode(newJoin);
      }

      public Node getNextNodeAfterGoto() { return getNextConsecutiveNode(); }
   }
}
