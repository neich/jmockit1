/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.testng;

import java.util.*;

import org.testng.annotations.*;

import mockit.*;
import mockit.integration.*;

// These tests are expected to fail, so they are kept inactive.
@Test(enabled = false)
public final class TestNGViolatedExpectationsTest
{
   @Test // fails with a "missing invocation" error
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_1(@Mocked Collaborator mock)
   {
      new CollaboratorExpectations(mock);
   }

   @Test // fails with the exception thrown by tested code
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_2(@Mocked Collaborator mock)
   {
      new CollaboratorExpectations(mock);

      mock.doSomething();
   }

   @Test // fails with an "unexpected invocation" error
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_3(@Mocked Collaborator mock)
   {
      new CollaboratorExpectations(mock);

      new Collaborator();
      new Collaborator();
   }

   // fails with a "missing invocation" error after the exception thrown by tested code
   @Test(expectedExceptions = IllegalFormatCodePointException.class)
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_4(@Mocked Collaborator mock)
   {
      new CollaboratorExpectations(mock);

      mock.doSomething();
   }

   @Test(expectedExceptions = AssertionError.class) // fails with a different exception than expected
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_5(@Mocked Collaborator mock)
   {
      new CollaboratorExpectations(mock);

      mock.doSomething();
   }

   @Test(expectedExceptions = AssertionError.class) // fails without the expected exception being thrown
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_6(@Mocked Collaborator mock)
   {
      new CollaboratorExpectations(mock);
   }
}
