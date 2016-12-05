/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.paths;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

final class PathBuilder
{
   private PathBuilder() {}

   @Nonnull
   static List<Path> buildPaths(@Nonnull List<Node> origNodes)
   {
      if (origNodes.size() <= 1) return Collections.emptyList();

      if (origNodes.get(0).line == origNodes.get(origNodes.size()-1).line) return Collections.emptyList();

      final List<Node> nodes = simplifyGraph(origNodes);

      if (nodes.size() == 1) {
         return Collections.emptyList();
      }

      List<Path> paths = new ArrayList<Path>();
      for (Node node: nodes) {
         paths.addAll(getAllPrimePathsFromNode(node));
      }

      if (paths.size() == 1) return new ArrayList<Path>();

      Node entry = nodes.get(0);
      entry.setPrimePaths(paths);

      return paths;
      // return getAllPathsFromExitNodes(nodes);
   }

   private static Stack<Node> simplifyGraph(List<Node> origNodes) {
      Stack<Node> nodes = new Stack<>();

      for (Node n : origNodes) {
         if (n.getSubsumedBy() != null) continue;

         if (n.isDummy()) continue;

         if (n.isEntry()) {
            nodes.add(n);
            continue;
         }

         if (n.getIncomingNodes().size() == 0) {
            n.getNextConsecutiveNode().getIncomingNodes().remove(n);
            if (n.isFork())
               for (Node child : n.getJumpNodes()) {
                  child.getIncomingNodes().remove(n);
               }
            continue;
         }

         Node prev = n.getIncomingNodes().get(0);

         if (!prev.isFork() && !n.hasMultipleEntries()) {
            if (n.isSubsumable()) {
               prev.subsumeNext(n);
               continue;
            } else if (prev.isSubsumable()){
               n.subsumePrev(prev);
               nodes.remove(prev);
               nodes.add(n);
            } else {
               nodes.add(n);
            }
         } else {
            nodes.add(n);
         }

      }

      return nodes;
   }

   private static List<Path> getAllPrimePathsFromNode(Node node) {
      // if (node instanceof Exit) return new ArrayList<Path>();
      if (node.getNextConsecutiveNode() == null) return new ArrayList<Path>();

      Path path = new Path(node);

      return getAllPrimePathsFromPath(path);
   }

   private static List<Path> getAllPrimePathsFromPath(Path path) {
      ArrayList<Path> paths = new ArrayList<Path>();

      Node lastNode = path.nodes.lastElement();

      if (lastNode.isExit()) {
         if (path.isPrime()) {
            paths.add(path);
            return paths;
         } else
            return paths;
      }

      // This should never happen ...
      if (lastNode.getNextConsecutiveNode() == null) return new ArrayList<Path>();

      int pos = path.nodes.indexOf(lastNode);
      if (path.nodes.size() > 1 && pos < path.nodes.size()-1) {
         if (pos == 0) {
            paths.add(path);
            return paths;
         }
         else if (path.nodes.firstElement().isEntry()) {
            path.nodes.pop();
            paths.add(path);
            return paths;
         }
         else
            return new ArrayList<>();
      } else {
         Path pcons = new Path(path, false);
         pcons.addNode(lastNode.getNextConsecutiveNode());
         paths.addAll(getAllPrimePathsFromPath(pcons));
         if (lastNode.isFork()) {
            for (Node n : lastNode.getJumpNodes()) {
               Path p = new Path(path, false);
               p.addNode(n);
               paths.addAll(getAllPrimePathsFromPath(p));
            }
         }
         return paths;
      }
   }
}
