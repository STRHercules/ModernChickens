package net.minecraft.world.level.validation;

import java.nio.file.Path;

public record ForbiddenSymlinkInfo(Path link, Path target) {
   public ForbiddenSymlinkInfo(Path param1, Path param2) {
      super();
      this.link = var1;
      this.target = var2;
   }

   public Path link() {
      return this.link;
   }

   public Path target() {
      return this.target;
   }
}
