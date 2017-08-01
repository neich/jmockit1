/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.primepaths;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

final class PPathBuilder
{
   private PPathBuilder() {}

   @Nonnull
   static List<PPath> buildPaths(@Nonnull List<PPNode> origNodes)
   {
      if (origNodes.size() <= 1) return Collections.emptyList();

      if (origNodes.get(0).line == origNodes.get(origNodes.size()-1).line) return Collections.emptyList();

      final List<PPNode> nodes = simplifyGraph(origNodes);

      if (nodes.size() == 1) {
         return Collections.emptyList();
      }

      List<PPath> paths = new ArrayList<PPath>();
      for (PPNode node: nodes) {
         paths.addAll(getAllPrimePathsFromNode(node));
      }

      if (paths.size() == 1) return new ArrayList<PPath>();

      PPNode entry = nodes.get(0);
      entry.setPrimePaths(paths);

      return paths;
      // return getAllPathsFromExitNodes(nodes);
   }

   private static Stack<PPNode> simplifyGraph(List<PPNode> origNodes) {
      Stack<PPNode> nodes = new Stack<>();

      for (PPNode n : origNodes) {
         if (n.getSubsumedBy() != null) continue;

         if (n.isDummy()) continue;

         if (n.isEntry()) {
            nodes.add(n);
            continue;
         }

         if (n.getIncomingNodes().size() == 0) {
            n.getNextConsecutiveNode().getIncomingNodes().remove(n);
            if (n.isFork())
               for (PPNode child : n.getJumpNodes()) {
                  child.getIncomingNodes().remove(n);
               }
            continue;
         }

         PPNode prev = n.getIncomingNodes().get(0);

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

   private static List<PPath> getAllPrimePathsFromNode(PPNode node) {
      // if (node instanceof Exit) return new ArrayList<Path>();
      if (node.getNextConsecutiveNode() == null) return new ArrayList<PPath>();

      PPath path = new PPath(node);

      return getAllPrimePathsFromPath(path);
   }

   private static List<PPath> getAllPrimePathsFromPath(PPath path) {
      ArrayList<PPath> paths = new ArrayList<PPath>();

      PPNode lastNode = path.nodes.lastElement();

      if (lastNode.isExit()) {
         if (path.isPrime()) {
            paths.add(path);
            return paths;
         } else
            return paths;
      }

      // This should never happen ...
      if (lastNode.getNextConsecutiveNode() == null) return new ArrayList<PPath>();

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
         PPath pcons = new PPath(path, false);
         pcons.addNode(lastNode.getNextConsecutiveNode());
         paths.addAll(getAllPrimePathsFromPath(pcons));
         if (lastNode.isFork()) {
            for (PPNode n : lastNode.getJumpNodes()) {
               PPath p = new PPath(path, false);
               p.addNode(n);
               paths.addAll(getAllPrimePathsFromPath(p));
            }
         }
         return paths;
      }
   }
}
