package net.minecraft.commands.execution;

import net.minecraft.commands.CommandResultCallback;

public record Frame(int depth, CommandResultCallback returnValueConsumer, Frame.FrameControl frameControl) {
   public Frame(int param1, CommandResultCallback param2, Frame.FrameControl param3) {
      super();
      this.depth = var1;
      this.returnValueConsumer = var2;
      this.frameControl = var3;
   }

   public void returnSuccess(int var1) {
      this.returnValueConsumer.onSuccess(var1);
   }

   public void returnFailure() {
      this.returnValueConsumer.onFailure();
   }

   public void discard() {
      this.frameControl.discard();
   }

   public int depth() {
      return this.depth;
   }

   public CommandResultCallback returnValueConsumer() {
      return this.returnValueConsumer;
   }

   public Frame.FrameControl frameControl() {
      return this.frameControl;
   }

   @FunctionalInterface
   public interface FrameControl {
      void discard();
   }
}
