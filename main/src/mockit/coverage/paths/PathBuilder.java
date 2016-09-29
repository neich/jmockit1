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

final class PathBuilder
{
   private PathBuilder() {}

   @Nonnull
   static List<Path> buildPaths(@Nonnull List<Node> nodes)
   {
      if (nodes.size() <= 1) return Collections.emptyList();

      if (!(nodes.get(0) instanceof Node.Entry)) return Collections.emptyList();

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

   private static List<Path> getAllPrimePathsFromNode(Node node) {
      if (node instanceof Exit) return new ArrayList<Path>();
      if (!(node instanceof Node.Fork) && node.getIncomingNodes().size() == 1 && !(node.getIncomingNodes().get(0) instanceof Node.Fork)) return new ArrayList<Path>();
      if (!(node instanceof Node.Goto) && node.getNextConsecutiveNode() == null) return new ArrayList<Path>();

      Path path = new Path(node);

      return getAllPrimePathsFromPath(path);
   }

   private static List<Path> getAllPrimePathsFromPath(Path path) {
      ArrayList<Path> paths = new ArrayList<Path>();

      Node lastNode = path.nodes.get(path.nodes.size() - 1);

      if (lastNode instanceof Exit)
         if (path.isPrime()) {
         paths.add(path);
         return paths;
      }
      else
         return paths;

      if (!(lastNode instanceof Node.Goto) && lastNode.getNextConsecutiveNode() == null) return new ArrayList<Path>();

      if (path.nodes.size() > 1) {
         int pos = path.nodes.indexOf(lastNode);
         if (pos == 0) {
            paths.add(path);
            return paths;
         } else if (pos < path.nodes.size() - 1) {
            return new ArrayList<Path>();
         }
      }

      if (lastNode instanceof Node.SimpleFork) {
         Node.SimpleFork lastSimpleFork = (Node.SimpleFork)lastNode;
         Path path1 = new Path(path, false);
         path1.addNode(lastSimpleFork.getNextConsecutiveNode());
         Path path2 = new Path(path, false);
         path2.addNode(lastSimpleFork.getNextNodeAfterJump());
         List<Path> pathsTotal = getAllPrimePathsFromPath(path1);
         pathsTotal.addAll(getAllPrimePathsFromPath(path2));
         return pathsTotal;
      } else if (lastNode instanceof Node.Goto) {
         path.addNode(((Node.Goto)lastNode).getNextNodeAfterGoto());
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
