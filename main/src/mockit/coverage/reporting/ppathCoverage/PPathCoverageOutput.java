/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting.ppathCoverage;

import mockit.coverage.CoveragePercentage;
import mockit.coverage.primepaths.PPMethodCoverageData;
import mockit.coverage.primepaths.PPath;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public final class PPathCoverageOutput
{
   @Nonnull private final PrintWriter output;
   @Nonnull private final PPathCoverageFormatter pathFormatter;
   @Nonnull private final Iterator<PPMethodCoverageData> nextMethod;

   // Helper fields:
   @Nullable private PPMethodCoverageData currentMethod;

   public PPathCoverageOutput(@Nonnull PrintWriter output, @Nonnull Collection<PPMethodCoverageData> methods)
   {
      this.output = output;
      pathFormatter = new PPathCoverageFormatter(output);
      nextMethod = methods.iterator();
      moveToNextMethod();
   }

   private void moveToNextMethod()
   {
      currentMethod = nextMethod.hasNext() ? nextMethod.next() : null;
   }

   public void writePathCoverageInfoIfLineStartsANewMethodOrConstructor(int lineNumber)
   {
      if (currentMethod != null && lineNumber == currentMethod.getFirstLineInBody()) {
         writePathCoverageInformationForMethod(currentMethod);
         moveToNextMethod();
      }
   }

   private void writePathCoverageInformationForMethod(@Nonnull PPMethodCoverageData methodData)
   {
      List<PPath> paths = methodData.getPaths();

      if (paths.size() > 1) {
         writeHeaderForAllPaths(methodData);
         pathFormatter.writeInformationForEachPath(paths);
         // pathFormatter.writeInformationForEachPath(paths);
         writeFooterForAllPaths();
      }
   }

   private void writeHeaderForAllPaths(@Nonnull PPMethodCoverageData methodData)
   {
      int coveredPaths = methodData.getCoveredPaths();
      int totalPaths = methodData.getTotalPaths();

      output.println("    <tr>");
      output.write("      <td></td><td class='count'>");
      output.print(methodData.getExecutionCount());
      output.println("</td>");
      output.println("      <td class='paths'>");
      output.write("        <span style='cursor:default; background-color:#");
      output.write(CoveragePercentage.percentageColor(coveredPaths, totalPaths));
      output.write("' onclick='hidePath()'>Path coverage: ");
      output.print(coveredPaths);
      output.print('/');
      output.print(totalPaths);
      output.println("</span>");
   }

   private void writeFooterForAllPaths()
   {
      output.println("      </td>");
      output.println("      <td class=\"paths\"></td>");
      output.println("    </tr>");
   }
}
