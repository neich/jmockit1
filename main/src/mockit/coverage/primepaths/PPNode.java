/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.primepaths;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ClassReferencesSubclass")
public class PPNode implements Serializable
{

   private boolean subsumable = false;

   public void setSubsumable(boolean subsumable) {
      this.subsumable = subsumable;
   }

   public boolean isSubsumable() {
      return subsumable;
   }

   @Nullable
   public List<PPath> getPrimePaths() {
      return primePaths;
   }

   public void setPrimePaths(@Nullable List<PPath> primePaths) {
      this.primePaths = primePaths;
   }


   public boolean isDummy() {
      return incomingNodes.size() == 0 && nextConsecutiveNode == null;
   }

   public boolean hasMultipleEntries() {
      return incomingNodes.size() > 1;
   }

   public boolean isEntry() {
      return false;
   }

   public boolean isExit() { return false; }

   public boolean isGoto() { return false; }

   public boolean isRegular() { return false; }

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
   protected List<LineSegment> extraLineSegments = new ArrayList<LineSegment>();
   protected List<PPNode> incomingNodes = new ArrayList<PPNode>();
   protected PPNode subsumedBy = null;
   private boolean isGoto = false;

   @Nullable protected PPNode nextConsecutiveNode;
   @Nullable protected List<PPNode> jumpNodes;

   @Nullable private List<PPath> primePaths;

   public PPNode(int line) {
      this.line = line;
   }

   public boolean isSimplified() {
      return subsumedBy != null;
   }

   public void setSubsumedBy(PPNode n) { this.subsumedBy = n; }

   public PPNode getSubsumedBy() { return subsumedBy; }

   public boolean isFork() { return jumpNodes != null; }

   @Nullable
   public PPNode getNextConsecutiveNode() {
      return nextConsecutiveNode;
   }

   public void addSuccessor(PPNode n) {}

   public List<PPNode> getJumpNodes() { return jumpNodes; }

   public void swapIncomingNode(PPNode oldNode, PPNode newNode) {
      incomingNodes.set(incomingNodes.indexOf(oldNode), newNode);
   }

   public void setNextConsecutiveNode(@Nullable PPNode nextConsecutiveNode) {
      this.nextConsecutiveNode = nextConsecutiveNode;
      nextConsecutiveNode.addIncomingNode(this);
   }

   public void swapNextConsecutiveNode(PPNode newNode) {

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

   public void subsumeNext(PPNode next) {
      this.extraLineSegments.addAll(next.getExtraLineSegments());
      next.removeIncomingNode(this);
      this.extraLineSegments.add(new LineSegment(nextConsecutiveNode.line, nextConsecutiveNode.segment));
      nextConsecutiveNode = next.getNextConsecutiveNode();
      nextConsecutiveNode.removeIncomingNode(next);
      nextConsecutiveNode.addIncomingNode(this);
      next.setSubsumedBy(this);
   }

   public void subsumePrev(PPNode prev) {
      for (PPNode n : prev.getIncomingNodes()) {
         if (n.getNextConsecutiveNode() == prev) n.setNextConsecutiveNode(this);
         else if (n.isFork() && n.getJumpNodes().contains(prev)) {
            n.getJumpNodes().remove(prev);
            n.getJumpNodes().add(this);
         }
      }
      this.removeIncomingNode(prev);
      this.moveIncomingNodes(prev);
      this.extraLineSegments.addAll(prev.getExtraLineSegments());
      prev.setSubsumedBy(this);
   }

   private void moveIncomingNodes(PPNode node) {
      this.incomingNodes.clear();
      for (PPNode n: node.getIncomingNodes()) {
         this.addIncomingNode(n);
         n.replaceJumpNode(node, this);
      }
   }

   private void removeIncomingNode(PPNode node) {
      incomingNodes.remove(node);
   }

   void setSegmentAccordingToPrecedingNode(@Nonnull PPNode precedingNode)
   {
      int currentSegment = precedingNode.segment;
      segment = currentSegment + 1; // precedingNode.isFork() ? currentSegment + 1 : currentSegment;
   }

   public final int getSegment() { return segment; }

   public List<LineSegment> getExtraLineSegments() { return this.extraLineSegments; }

   final void setReached(@Nullable Boolean reached) { this.reached.set(reached); }
   final boolean wasReached() { return reached.get() != null; }

   @Override
   public final String toString() {
      String baseName = getClass().getName();
      baseName = baseName.substring(baseName.indexOf("$")+1, baseName.length());
/*
      if (isEntry()) baseName = "Entry";
      else if (isExit()) baseName = "Exit";
      else if (isFork()) baseName = "Fork";
      else baseName = "Block";
*/
      return baseName + ':' + line + '-' + segment;
   }

   public void addIncomingNode(PPNode node) {
      incomingNodes.add(node);
   }

   public List<PPNode> getIncomingNodes() { return this.incomingNodes; }

   void addNextNode(@Nonnull PPNode nextNode) {
      if (jumpNodes == null) jumpNodes = new ArrayList<>();
      jumpNodes.add(nextNode);
      nextNode.addIncomingNode(this);
   }

   public void replaceNextConsecutiveNode(PPNode oldNode, PPNode newNode) {
      if (nextConsecutiveNode == oldNode) nextConsecutiveNode = newNode;
      else if (jumpNodes.contains(oldNode)) {
         jumpNodes.remove(oldNode);
         jumpNodes.add(newNode);
      }
   }

   void replaceJumpNode(@Nonnull PPNode oldNode, @Nonnull PPNode nextNode) {
/*
      if (jumpNodes != null) {
         nextNodeAfterJump.removeIncomingNode(this);
         nextNode.removeIncomingNode(nextNodeAfterJump);
         nextNode.addIncomingNode(this);
         this.extraLineSegmentsForJump.add(new LineSegment(nextNodeAfterJump.line, nextNodeAfterJump.segment));
      } else {
         jum
      }
      nextNodeAfterJump = nextNode;
*/
   }

   public static class Entry extends PPNode {
      public Entry(int line) {
         super(line);
         setSubsumable(false);
      }

      public boolean isEntry() {  return true; }
      public boolean isRegular() {  return true; }
   }

   public static class Exit extends PPNode {
      public Exit(int line) {
         super(line);
         setSubsumable(false);
      }

      public boolean isExit() {  return true; }
      public boolean isRegular() {  return true; }
   }

   public static class Fork extends PPNode {
      public Fork(int line) {
         super(line);
         setSubsumable(false);
         jumpNodes = new ArrayList<>();
      }

      public void addSuccessor(PPNode n) {
         jumpNodes.add(n);
         n.addIncomingNode(this);
      }
   }

   public static class Join extends PPNode {
      public Join(int line) {
         super(line);
         setSubsumable(true);
      }

      public boolean isRegular() {  return true; }
   }

   public static class BasicBlock extends PPNode {
      public BasicBlock(int line) {
         super(line);
         setSubsumable(true);
      }

      public boolean isRegular() {  return true; }
   }

   public static class Goto extends PPNode {
      public Goto(int line) {
         super(line);
         setSubsumable(true);
      }

      public boolean isGoto() {  return true; }
      public void addSuccessor(PPNode n) {
         setNextConsecutiveNode(n);
         n.addIncomingNode(this);
      }

   }

}

