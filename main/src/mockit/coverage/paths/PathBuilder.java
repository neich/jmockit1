/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.paths;

import mockit.coverage.paths.Node.Exit;

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

      if (!(origNodes.get(0) instanceof Node.Entry)) return Collections.emptyList();

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

      Node.Entry entry = (Node.Entry)nodes.get(0);
      entry.primePaths = paths;

      return paths;
      // return getAllPathsFromExitNodes(nodes);
   }

   private static Stack<Node> simplifyGraph(List<Node> origNodes) {
      Stack<Node> nodes = new Stack<>();

      for (Node n : origNodes) {
         if (n instanceof Node.Entry) {
            nodes.add(n);
            continue;
         }
         if (n.getIncomingNodes().size() == 0) continue;

         Node prev = n.getIncomingNodes().get(0);

         if (prev instanceof Node.Fork || n.getIncomingNodes().size() > 1) {
            nodes.add(n);
            continue;
         }

         if (!(prev instanceof Node.Entry)) {
            if (!prev.fuse(n)) {
               nodes.remove(prev);
               prev.setSubsumedBy(n);
               nodes.push(n);
            } else {
               n.setSubsumedBy(prev);
            }
         } else nodes.push(n);
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

      if (lastNode instanceof Exit) {
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
         else if (path.nodes.firstElement() instanceof Node.Entry) {
            path.nodes.pop();
            paths.add(path);
            return paths;
         }
         else
            return new ArrayList<>();
      } else if (lastNode instanceof Node.SimpleFork) {
         Node.SimpleFork lastSimpleFork = (Node.SimpleFork) lastNode;
         Path path1 = new Path(path, false);
         path1.addNode(lastSimpleFork.getNextConsecutiveNode());
         Path path2 = new Path(path, false);
         path2.addNode(lastSimpleFork.getNextNodeAfterJump());
         List<Path> pathsTotal = getAllPrimePathsFromPath(path1);
         pathsTotal.addAll(getAllPrimePathsFromPath(path2));
         return pathsTotal;
      } else if (lastNode instanceof Node.Goto) {
         path.addNode(((Node.Goto) lastNode).getNextNodeAfterGoto());
         return getAllPrimePathsFromPath(path);
      } else {
         path.addNode(lastNode.getNextConsecutiveNode());
         return getAllPrimePathsFromPath(path);
      }
   }

   @Nonnull
   private static List<Path> getAllPathsFromExitNodes(@Nonnull List<Node> nodes)
   {
      List<Path> paths = new ArrayList<Path>();

      for (Node node : nodes) {
         if (node instanceof Exit) {
            paths.addAll(((Exit) node).paths);
         }
      }

      return paths;
   }
}
