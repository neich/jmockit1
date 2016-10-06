/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting.pathCoverage;

import mockit.coverage.paths.Node;
import mockit.coverage.paths.Path;

import javax.annotation.Nonnull;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;

final class PathCoverageFormatter
{
   @Nonnull private final PrintWriter output;
   @Nonnull private final StringBuilder lineSegmentIds;
   private char pathId1;
   private char pathId2;

   PathCoverageFormatter(@Nonnull PrintWriter output)
   {
      this.output = output;
      lineSegmentIds = new StringBuilder(100);
   }

   void writeInformationForEachPath(@Nonnull List<Path> paths)
   {
      pathId1 = 'A';
      pathId2 = '\0';

      for (Path path : paths) {
         writeCoverageInfoForIndividualPath(path);

         if (pathId2 == '\0' && pathId1 < 'Z') {
            pathId1++;
         }
         else if (pathId2 == '\0') {
            pathId1 = 'A';
            pathId2 = 'A';
         }
         else if (pathId2 < 'Z') {
            pathId2++;
         }
         else {
            pathId1++;
            pathId2 = 'A';
         }
      }
   }

   private void writeCoverageInfoForIndividualPath(@Nonnull Path path)
   {
      int executionCount = path.getExecutionCount();
      String lineSegmentIdsForPath = getIdsForLineSegmentsBelongingToThePath(path);

      output.write("        <span class='");
      output.write(executionCount == 0 ? "uncovered" : "covered");
      output.write("' onclick=\"showPath(this,");
      output.write(lineSegmentIdsForPath);
      output.write(")\">");
      writePathId();
      output.write(": ");
      output.print(executionCount);
      output.println("</span>");
   }

   @Nonnull
   private String getIdsForLineSegmentsBelongingToThePath(@Nonnull Path path)
   {
      lineSegmentIds.setLength(0);
      lineSegmentIds.append('[');

      int previousLine = 0;
      int previousSegment = 0;

      Iterator<Node> it = path.getNodes().iterator();
      while (it.hasNext()) {
         Node node = it.next();
         lineSegmentIds.append(MessageFormat.format("[''{0}'', ", node.toString()));
         lineSegmentIds.append('\'');

         int line = node.line;
         int segment = node.getSegment();

         appendSegmentId(line, segment, false);

         for (Node.LineSegment ls: node.getExtraLineSegments()) appendSegmentId(ls.line, ls.segment, true);
         lineSegmentIds.append("\']");
         if (it.hasNext()) lineSegmentIds.append(',');
      }
      lineSegmentIds.append(']');

      return lineSegmentIds.toString();
   }

   private void appendSegmentId(int line, int segment, boolean space)
   {
      if (space) {
         lineSegmentIds.append(' ');
      }
      
      lineSegmentIds.append('l').append(line).append('s').append(segment);
   }

   private void writePathId()
   {
      output.write(pathId1);

      if (pathId2 != '\0') {
         output.write(pathId2);
      }
   }
}
