/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.applet.*;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.*;

public final class MisusedFakingAPITest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @Test
   public void fakeSameMethodTwiceWithReentrantFakesFromTwoDifferentFakeClasses()
   {
      new MockUp<Applet>() {
         @Mock
         int getComponentCount(Invocation inv)
         {
            int i = inv.proceed();
            return i + 1;
         }
      };

      int i = new Applet().getComponentCount();
      assertEquals(1, i);

      new MockUp<Applet>() {
         @Mock
         int getComponentCount(Invocation inv)
         {
            int j = inv.proceed();
            return j + 2;
         }
      };

      // Should return 3, but returns 5. Chaining mock methods is not supported.
      int j = new Applet().getComponentCount();
      assertEquals(5, j);
   }

   public interface AnInterface { void doSomething(); int getValue(); }

   @Test
   public void attemptToProceedIntoEmptyMethodOfPublicInterface()
   {
      thrown.expect(UnsupportedOperationException.class);
      thrown.expectMessage("Cannot proceed");
      thrown.expectMessage("interface method");

      AnInterface faked = new MockUp<AnInterface>() {
         @Mock
         void doSomething(Invocation invocation) { invocation.proceed(); }
      }.getMockInstance();

      faked.doSomething();
   }

   static final class AppletFake extends MockUp<Applet> {
      final int componentCount;
      AppletFake(int componentCount) { this.componentCount = componentCount; }
      @Mock int getComponentCount(Invocation inv) { return componentCount; }
   }

   @Test
   public void applyTheSameFakeForAClassTwice()
   {
      new AppletFake(1);
      new AppletFake(2); // second application has no effect

      assertEquals(1, new Applet().getComponentCount());
   }

   static final class InterfaceFake extends MockUp<AnInterface> {
      final int value;
      InterfaceFake(int value) { this.value = value; }
      @Mock int getValue(Invocation inv) { return value; }
   }

   @Test
   public void applyTheSameFakeForAnInterfaceTwice()
   {
      AnInterface instance1 = new InterfaceFake(1).getMockInstance();
      AnInterface instance2 = new InterfaceFake(2).getMockInstance(); // second application has no effect

      assertEquals(1, instance1.getValue());
      assertEquals(1, instance2.getValue());
   }
}