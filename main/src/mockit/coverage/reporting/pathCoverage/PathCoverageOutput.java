/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting.pathCoverage;

import mockit.coverage.CoveragePercentage;
import mockit.coverage.paths.MethodCoverageData;
import mockit.coverage.paths.Path;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public final class PathCoverageOutput
{
   @Nonnull private final PrintWriter output;
   @Nonnull private final PathCoverageFormatter pathFormatter;
   @Nonnull private final Iterator<MethodCoverageData> nextMethod;

   // Helper fields:
   @Nullable private MethodCoverageData currentMethod;

   public PathCoverageOutput(@Nonnull PrintWriter output, @Nonnull Collection<MethodCoverageData> methods)
   {
      this.output = output;
      pathFormatter = new PathCoverageFormatter(output);
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

   private void writePathCoverageInformationForMethod(@Nonnull MethodCoverageData methodData)
   {
      List<Path> paths = methodData.getPaths();

      if (paths.size() > 1) {
         writeHeaderForAllPaths(methodData);
         pathFormatter.writeInformationForEachPath(paths);
         // pathFormatter.writeInformationForEachPath(paths);
         writeFooterForAllPaths();
      }
   }

   private void writeHeaderForAllPaths(@Nonnull MethodCoverageData methodData)
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
