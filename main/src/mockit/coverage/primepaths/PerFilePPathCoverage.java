/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.primepaths;

import mockit.coverage.CoveragePercentage;
import mockit.coverage.data.PerFileCoverage;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;

public final class PerFilePPathCoverage implements PerFileCoverage
{
   private static final long serialVersionUID = 6075064821486644269L;

   @Nonnull
   public final Map<Integer, PPMethodCoverageData> firstLineToMethodData = new HashMap<Integer, PPMethodCoverageData>();

   // Computed on demand:
   private transient int totalPaths;
   private transient int coveredPaths;

   public PerFilePPathCoverage() { initializeCache(); }
   private void initializeCache() { totalPaths = coveredPaths = -1; }

   private void readObject(@Nonnull ObjectInputStream in) throws IOException, ClassNotFoundException
   {
      initializeCache();
      in.defaultReadObject();
   }

   public void addMethod(@Nonnull PPMethodCoverageData methodData)
   {
      int firstLineInBody = methodData.getFirstLineInBody();
      firstLineToMethodData.put(firstLineInBody, methodData);
   }

   public int registerExecution(int firstLineInMethodBody, int node)
   {
      PPMethodCoverageData methodData = firstLineToMethodData.get(firstLineInMethodBody);

      if (methodData != null) {
         return methodData.markNodeAsReached(node);
      }

      return -1;
   }

   @Override
   public int getTotalItems()
   {
      computeValuesIfNeeded();
      return totalPaths;
   }

   @Override
   public int getCoveredItems()
   {
      computeValuesIfNeeded();
      return coveredPaths;
   }

   @Override
   public int getCoveragePercentage()
   {
      computeValuesIfNeeded();
      return CoveragePercentage.calculate(coveredPaths, totalPaths);
   }

   private void computeValuesIfNeeded()
   {
      if (totalPaths >= 0) return;

      totalPaths = coveredPaths = 0;

      for (PPMethodCoverageData method : firstLineToMethodData.values()) {
         totalPaths += method.getTotalPaths();
         coveredPaths += method.getCoveredPaths();
      }
   }

   public void reset()
   {
      for (PPMethodCoverageData methodData : firstLineToMethodData.values()) {
         methodData.reset();
      }

      initializeCache();
   }

   public void mergeInformation(@Nonnull PerFilePPathCoverage previousCoverage)
   {
      Map<Integer, PPMethodCoverageData> previousInfo = previousCoverage.firstLineToMethodData;
      addExecutionCountsFromPreviousTestRun(previousInfo);
      addPathInfoFromPreviousTestRunForMethodsNotExecutedInCurrentTestRun(previousInfo);
   }

   private void addExecutionCountsFromPreviousTestRun(@Nonnull Map<Integer, PPMethodCoverageData> previousInfo)
   {
      for (Map.Entry<Integer, PPMethodCoverageData> firstLineAndInfo : firstLineToMethodData.entrySet()) {
         Integer firstLine = firstLineAndInfo.getKey();
         PPMethodCoverageData previousPathInfo = previousInfo.get(firstLine);

         if (previousPathInfo != null) {
            PPMethodCoverageData pathInfo = firstLineAndInfo.getValue();
            pathInfo.addCountsFromPreviousTestRun(previousPathInfo);
         }
      }
   }

   private void addPathInfoFromPreviousTestRunForMethodsNotExecutedInCurrentTestRun(
      @Nonnull Map<Integer, PPMethodCoverageData> previousInfo)
   {
      for (Map.Entry<Integer, PPMethodCoverageData> firstLineAndInfo : previousInfo.entrySet()) {
         Integer firstLine = firstLineAndInfo.getKey();

         if (!firstLineToMethodData.containsKey(firstLine)) {
            PPMethodCoverageData pathInfo = firstLineAndInfo.getValue();
            firstLineToMethodData.put(firstLine, pathInfo);
         }
      }
   }
}
