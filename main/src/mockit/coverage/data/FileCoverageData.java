/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.data;

import mockit.coverage.Metrics;
import mockit.coverage.TestRun;
import mockit.coverage.dataItems.PerFileDataCoverage;
import mockit.coverage.lines.PerFileLineCoverage;
import mockit.coverage.paths.MethodCoverageData;
import mockit.coverage.paths.PerFilePathCoverage;
import mockit.coverage.primepaths.PPMethodCoverageData;
import mockit.coverage.primepaths.PerFilePPathCoverage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;

import static mockit.coverage.Metrics.*;

/**
 * Coverage data gathered for the lines and branches of a single source file.
 */
public final class FileCoverageData implements Serializable
{
   private static final long serialVersionUID = 3508572808457541012L;

   @Nonnull private static final PerFileLineCoverage NO_LINE_INFO = new PerFileLineCoverage();
   @Nonnull private static final PerFilePathCoverage NO_PATH_INFO = new PerFilePathCoverage();
   @Nonnull private static final PerFilePPathCoverage NO_PRIMEPATH_INFO = new PerFilePPathCoverage();
   @Nonnull private static final PerFileDataCoverage NO_DATA_INFO = new PerFileDataCoverage();

   @Nonnull public PerFileLineCoverage lineCoverageInfo;
   @Nonnull public PerFilePathCoverage pathCoverageInfo;
   @Nonnull public PerFilePPathCoverage ppathCoverageInfo;
   @Nonnull public PerFileDataCoverage dataCoverageInfo;

   // Used for fast indexed access.
   public final int index;

   // Used for output styling in the HTML report.
   @Nullable public String kindOfTopLevelType;

   // Used to track the last time the ".class" file was modified, to decide if merging can be done.
   long lastModified;

   private final boolean loadedAfterTestCompletion;

   public FileCoverageData(int index, @Nullable String kindOfTopLevelType)
   {
      this.index = index;
      this.kindOfTopLevelType = kindOfTopLevelType;
      lineCoverageInfo = LineCoverage.active ? new PerFileLineCoverage() : NO_LINE_INFO;
      pathCoverageInfo = PathCoverage.active ? new PerFilePathCoverage() : NO_PATH_INFO;
      ppathCoverageInfo = PrimePathCoverage.active ? new PerFilePPathCoverage() : NO_PRIMEPATH_INFO;
      dataCoverageInfo = DataCoverage.active ? new PerFileDataCoverage() : NO_DATA_INFO;
      loadedAfterTestCompletion = TestRun.isTerminated();
   }

   public boolean wasLoadedAfterTestCompletion() { return loadedAfterTestCompletion; }

   @Nonnull
   public PerFileLineCoverage getLineCoverageData() { return lineCoverageInfo; }

   public void addMethod(@Nonnull MethodCoverageData methodData) { pathCoverageInfo.addMethod(methodData); }

   public void addMethod(@Nonnull PPMethodCoverageData methodData) { ppathCoverageInfo.addMethod(methodData); }

   @Nonnull
   public Collection<MethodCoverageData> getMethods()
   {
      List<MethodCoverageData> methods =
         new ArrayList<MethodCoverageData>(pathCoverageInfo.firstLineToMethodData.values());

      Collections.sort(methods, new Comparator<MethodCoverageData>() {
         @Override
         public int compare(MethodCoverageData m1, MethodCoverageData m2)
         {
            int l1 = m1.getFirstLineInBody();
            int l2 = m2.getFirstLineInBody();

            if (l1 == l2) {
               return 0;
            }

            return l1 < l2 ? -1 : 1;
         }
      });

      return methods;
   }

   @Nonnull
   public Collection<PPMethodCoverageData> getPPMethods()
   {
      List<PPMethodCoverageData> methods =
          new ArrayList<PPMethodCoverageData>(ppathCoverageInfo.firstLineToMethodData.values());

      Collections.sort(methods, new Comparator<PPMethodCoverageData>() {
         @Override
         public int compare(PPMethodCoverageData m1, PPMethodCoverageData m2)
         {
            int l1 = m1.getFirstLineInBody();
            int l2 = m2.getFirstLineInBody();

            if (l1 == l2) {
               return 0;
            }

            return l1 < l2 ? -1 : 1;
         }
      });

      return methods;
   }


   @Nonnull
   public PerFileCoverage getPerFileCoverage(@Nonnull Metrics metric)
   {
      switch (metric) {
         case LineCoverage: return lineCoverageInfo;
         case PathCoverage: return pathCoverageInfo;
         case PrimePathCoverage: return ppathCoverageInfo;
         default: return dataCoverageInfo;
      }
   }

   public int getTotalItemsForAllMetrics()
   {
      int totalItems = 0;

      if (lineCoverageInfo != NO_LINE_INFO) {
         totalItems += lineCoverageInfo.getTotalItems();
      }

      if (pathCoverageInfo != NO_PATH_INFO) {
         totalItems += pathCoverageInfo.getTotalItems();
      }

      if (ppathCoverageInfo != NO_PRIMEPATH_INFO) {
         totalItems += ppathCoverageInfo.getTotalItems();
      }

      if (dataCoverageInfo != NO_DATA_INFO) {
         totalItems += dataCoverageInfo.getTotalItems();
      }

      return totalItems;
   }

   void mergeWithDataFromPreviousTestRun(@Nonnull FileCoverageData previousInfo)
   {
      if (lineCoverageInfo == NO_LINE_INFO) {
         lineCoverageInfo = previousInfo.lineCoverageInfo;
      }
      else if (previousInfo.lineCoverageInfo != NO_LINE_INFO) {
         lineCoverageInfo.mergeInformation(previousInfo.lineCoverageInfo);
      }

      if (pathCoverageInfo == NO_PATH_INFO) {
         pathCoverageInfo = previousInfo.pathCoverageInfo;
      }
      else if (previousInfo.pathCoverageInfo != NO_PATH_INFO) {
         pathCoverageInfo.mergeInformation(previousInfo.pathCoverageInfo);
      }

      if (ppathCoverageInfo == NO_PRIMEPATH_INFO) {
         ppathCoverageInfo = previousInfo.ppathCoverageInfo;
      }
      else if (previousInfo.ppathCoverageInfo != NO_PRIMEPATH_INFO) {
         ppathCoverageInfo.mergeInformation(previousInfo.ppathCoverageInfo);
      }

      if (dataCoverageInfo == NO_DATA_INFO) {
         dataCoverageInfo = previousInfo.dataCoverageInfo;
      }
      else if (previousInfo.dataCoverageInfo != NO_DATA_INFO) {
         dataCoverageInfo.mergeInformation(previousInfo.dataCoverageInfo);
      }
   }
}
