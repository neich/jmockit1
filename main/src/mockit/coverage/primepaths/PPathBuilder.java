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

final class PPathBuilder {
  private PPathBuilder() {
  }

  @Nonnull
  static List<PPath> buildPaths(@Nonnull List<PPNode> origNodes) {
    if (origNodes.size() <= 1) return Collections.emptyList();

    if (origNodes.get(0).line == origNodes.get(origNodes.size() - 1).line) return Collections.emptyList();

    final List<PPNode> nodes = simplifyGraph(origNodes);

    if (nodes.size() == 1) {
      return Collections.emptyList();
    }

    List<PPath> paths = getPrimePaths(nodes);

    if (paths.size() == 1) return new ArrayList<PPath>();

    PPNode entry = nodes.get(0);
    entry.setPrimePaths(paths);

    return paths;
    // return getAllPathsFromExitNodes(nodes);
  }

  private static Stack<PPNode> simplifyGraph(List<PPNode> origNodes) {
    Stack<PPNode> nodes = new Stack<PPNode>();

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
        } else if (prev.isSubsumable()) {
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

    int index = 0;
    for (PPNode node: nodes) {
      node.setIndex(index);
      ++index;
    }

    return nodes;
  }

  private static List<PPath> getPrimePaths(List<PPNode> nodes) {
    List<PPath> simplePaths = new ArrayList<PPath>();
    for (PPNode node : nodes) {
      simplePaths.add(new PPath(node));
    }

    List<PPath> primePaths = new ArrayList<PPath>();
    do {
      simplePaths = getPrimePathsFromSimplePaths(simplePaths, primePaths);
    } while (simplePaths.size() > 0);

    return primePaths;
  }

  private static List<PPath> getPrimePathsFromSimplePaths(List<PPath> simplePaths, List<PPath> primePaths) {
    List<PPath> newSimplePaths = new ArrayList<PPath>();
    for (PPath spath : simplePaths) {
      PPNode lastNode = spath.getLastNode();
      List<PPNode> nextNodes = new ArrayList<PPNode>();
      if (lastNode.nextConsecutiveNode != null)
        nextNodes.add(lastNode.nextConsecutiveNode);
      if (lastNode.jumpNodes != null)
        nextNodes.addAll(lastNode.jumpNodes);
      checkNewSimplePaths(primePaths, newSimplePaths, spath, nextNodes);
    }
    return newSimplePaths;
  }

  private static void checkNewSimplePaths(List<PPath> primePaths, List<PPath> newSimplePaths, PPath spath, List<PPNode> nextNodes) {
    boolean potentialPrime = true;
    for (PPNode node : nextNodes) {
      PPath path = new PPath(spath, false);
      path.addNode(node);
      if (path.isSimple()) {
        newSimplePaths.add(path);
        potentialPrime = false;
      }
    }
    if (potentialPrime && spath.isPrime())
      primePaths.add(spath);

  }
}
