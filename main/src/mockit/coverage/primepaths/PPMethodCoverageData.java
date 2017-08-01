/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.primepaths;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PPMethodCoverageData implements Serializable
{
   private static final long serialVersionUID = -5073393714435522417L;

   @Nonnull private List<PPNode> nodes;
   private int firstLine;
   private int lastLine;

   // Helper fields used during node building and path execution:
   @Nonnull private final transient ThreadLocal<List<PPNode>> testPath;
   @Nonnull private final transient ThreadLocal<Integer> previousNodeIndex;

   @Nonnull public List<PPath> paths;
   @Nonnull private List<PPath> nonShadowedPaths;

   public PPMethodCoverageData()
   {
      nodes = Collections.emptyList();
      paths = Collections.emptyList();
      nonShadowedPaths = Collections.emptyList();
      testPath = new ThreadLocal<List<PPNode>>();
      previousNodeIndex = new ThreadLocal<Integer>();
      clearNodes();
   }

   public void buildPaths(int lastExecutableLine, @Nonnull PPNodeBuilder nodeBuilder)
   {
      firstLine = nodeBuilder.firstLine;
      lastLine = lastExecutableLine;

      nodes = nodeBuilder.nodes;
      paths = PPathBuilder.buildPaths(nodes);
      buildListOfNonShadowedPaths();
   }

   private void buildListOfNonShadowedPaths()
   {
      nonShadowedPaths = new ArrayList<PPath>(paths.size());

      for (PPath path : paths) {
         if (!path.isShadowed()) {
            nonShadowedPaths.add(path);
         }
      }
   }

   public int getFirstLineInBody() { return firstLine; }
   public int getLastLineInBody() { return lastLine; }

   public int markNodeAsReached(int nodeIndex)
   {
      if (nodeIndex == 0) {
         clearNodes();
      }

      PPNode node = nodes.get(nodeIndex);
      List<PPNode> testPathNodes = testPath.get();

      if (node.isEntry() || node.getIncomingNodes().size() > 0) {
         PPNode n = node.isSimplified() ? node.subsumedBy : node;
         n.setReached(Boolean.TRUE);
         if (testPathNodes.size() == 0 || testPathNodes.get(testPathNodes.size()-1) != n)
            testPathNodes.add(n);
         if (!(node instanceof PPNode.Fork)) {
            PPNode next = node.getNextConsecutiveNode();
            if (next != null) {
               next.setReached(Boolean.TRUE);
               if (testPathNodes.size() == 0 || testPathNodes.get(testPathNodes.size()-1) != next)
                  testPathNodes.add(next);
            }
         }
      }

      int previousExecutionCount = -1;
      if (node.isExit()) {
         PPNode start = testPathNodes.get(0);
         if (start.isEntry() && start.getPrimePaths() != null) {

            for (PPath path : start.getPrimePaths()) {
               int previousExecutionCountPath = path.countExecutionIfAllNodesWereReached(testPathNodes);

               if (previousExecutionCountPath == 0) {
                  previousExecutionCount = 0;
               }
            }
         }
         return previousExecutionCount;
      }

      return -1;
   }

   private void clearNodes()
   {
      for (PPNode node : nodes) {
         node.setReached(null);
      }

      testPath.set(new ArrayList<PPNode>());
      previousNodeIndex.set(0);
   }

   @Nonnull public List<PPath> getPaths() { return nonShadowedPaths; }

   public int getExecutionCount()
   {
      int totalCount = 0;

      for (PPath path : nonShadowedPaths) {
         totalCount += path.getExecutionCount();
      }

      return totalCount;
   }

   public int getTotalPaths() { return nonShadowedPaths.size(); }

   public int getCoveredPaths()
   {
      int coveredCount = 0;

      for (PPath path : nonShadowedPaths) {
         if (path.getExecutionCount() > 0) {
            coveredCount++;
         }
      }

      return coveredCount;
   }

   public void addCountsFromPreviousTestRun(PPMethodCoverageData previousData)
   {
      for (int i = 0; i < paths.size(); i++) {
         PPath path = paths.get(i);
         PPath previousPath = previousData.paths.get(i);
         path.addCountFromPreviousTestRun(previousPath);
      }
   }

   public void reset()
   {
      clearNodes();

      for (PPath path : paths) {
         path.reset();
      }
   }
}
