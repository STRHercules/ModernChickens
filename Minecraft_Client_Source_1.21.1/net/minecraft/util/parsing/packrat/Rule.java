package net.minecraft.util.parsing.packrat;

import java.util.Optional;

public interface Rule<S, T> {
   Optional<T> parse(ParseState<S> var1);

   static <S, T> Rule<S, T> fromTerm(Term<S> var0, Rule.RuleAction<S, T> var1) {
      return new Rule.WrappedTerm(var1, var0);
   }

   static <S, T> Rule<S, T> fromTerm(Term<S> var0, Rule.SimpleRuleAction<T> var1) {
      return new Rule.WrappedTerm((var1x, var2) -> {
         return Optional.of(var1.run(var2));
      }, var0);
   }

   public static record WrappedTerm<S, T>(Rule.RuleAction<S, T> action, Term<S> child) implements Rule<S, T> {
      public WrappedTerm(Rule.RuleAction<S, T> param1, Term<S> param2) {
         super();
         this.action = var1;
         this.child = var2;
      }

      public Optional<T> parse(ParseState<S> var1) {
         Scope var2 = new Scope();
         return this.child.parse(var1, var2, Control.UNBOUND) ? this.action.run(var1, var2) : Optional.empty();
      }

      public Rule.RuleAction<S, T> action() {
         return this.action;
      }

      public Term<S> child() {
         return this.child;
      }
   }

   @FunctionalInterface
   public interface RuleAction<S, T> {
      Optional<T> run(ParseState<S> var1, Scope var2);
   }

   @FunctionalInterface
   public interface SimpleRuleAction<T> {
      T run(Scope var1);
   }
}
