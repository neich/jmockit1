/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.paths;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

public final class Path implements Serializable
{
   private static final long serialVersionUID = 8895491272907955543L;

   @Nonnull final Stack<Node> nodes = new Stack<>();
   @Nonnull private final AtomicInteger executionCount = new AtomicInteger();
   private final boolean shadowed;
   @Nullable private Path shadowPath;

   Path(@Nonnull Node node)
   {
      shadowed = false;
      addNode(node);
   }

   Path(@Nonnull Path sharedSubPath, boolean shadowed)
   {
      this.shadowed = shadowed;
      sharedSubPath.shadowPath = shadowed ? this : null;
      nodes.addAll(sharedSubPath.nodes);
   }

   void addNode(@Nonnull Node node) { nodes.add(node); }

   int countExecutionIfAllNodesWereReached(@Nonnull List<Node> nodesReached)
   {
      Node startPath = this.nodes.get(0);
      int posReached = nodesReached.indexOf(startPath);
      if (posReached < 0) return -1;

      boolean allNodesReached;

      int posPath = 0;
      while (true) {
         if (nodesReached.get(posReached) != this.nodes.get(posPath)) {
            while (nodesReached.get(posReached) != startPath) {
               posReached++;
               if (posReached >= nodesReached.size()) break;
            }
            if (posReached >= nodesReached.size()) {
               allNodesReached = false;
               break;
            }
            posPath = 0;
         }
         posPath++;
         if (posPath == this.nodes.size()) {
            allNodesReached = true;
            break;
         }
         posReached++;
         if (posReached == nodesReached.size()) {
            allNodesReached = false;
            break;
         }
      }


      if (allNodesReached) {
         return executionCount.getAndIncrement();
      }

      return -1;
   }

   public boolean isShadowed() { return shadowed; }
   @Nonnull public List<Node> getNodes() { return nodes; }

   public int getExecutionCount()
   {
      int count = executionCount.get();

      if (shadowPath != null) {
         count += shadowPath.executionCount.get();
      }

      return count;
   }

   void addCountFromPreviousTestRun(@Nonnull Path previousPath)
   {
      int currentExecutionCount = executionCount.get();
      int previousExecutionCount = previousPath.executionCount.get();
      executionCount.set(currentExecutionCount + previousExecutionCount);
   }

   void reset()
   {
      executionCount.set(0);
   }

   public boolean isPrime() {
      Node node = this.nodes.get(0);
      for (Node n: node.getIncomingNodes())
         if (!this.nodes.contains(n)) return false;
      return true;
   }
}
