package jorokr21.scandroid

import cats.evidence.Leibniz

import scala.reflect.ClassTag

/** Newtypes in Scala!
  *
  * Inspired by newtypes in Haskell.
  * Scala specific features:
  *  - A newtype `T` is a subtype of the original type (aka. translucent).
  *  - To avoid boxing primitives are special-cased.
  *  - Typeclass instances (implicits) should be defined in the "companion object".
  *
  * @example {{{
  *   type IntProd = IntProd.Self
  *   object IntProd extends Newtype.int {
  *     // Use the product monoid instead of the sum monoid without loss of coherence.
  *     implicit val monoid: Monoid[Self] = ???
  *   }
  * }}}
  *
  * @example {{{
  *   // Plain Double is not accepted as Kg.
  *   type Kg = Kg.Self
  *   object Kg extends Newtype.double {
  *     implicit class Ops(private val self: Self) extends AnyVal {
  *       def lbs: Lbs = ???
  *     }
  *   }
  * }}}
  */
sealed abstract class Newtype[A](implicit A: ClassTag[A]) {
  protected type Repr = A
  /** Ensure that implicits are in scope. */
  protected trait Scope[T] extends Any
  type Self <: Repr with Scope[this.type]
  def apply(rep: Repr): Self
  implicit def classTag: ClassTag[Self] = leibniz.substitute(A)
  implicit val leibniz: Leibniz[Repr, Self] = new Leibniz[Repr, Self] {
    def substitute[F[_]](fa: F[Repr]) = fa.asInstanceOf[F[Self]]
  }
}

object Newtype {

  /** Generic newtype, boxes primitives. */
  abstract class of[A: ClassTag] extends Newtype[A] {
    @inline final def apply(rep: Repr): Self = rep.asInstanceOf[Self]
  }

  /** Unboxed newtype for Booleans. */
  abstract class boolean extends Newtype[Boolean] {
    @inline final def apply(rep: Repr): Self = rep.asInstanceOf[Self]
  }

  /** Unboxed newtype for Bytes. */
  abstract class byte extends Newtype[Byte] {
    @inline final def apply(rep: Repr): Self = rep.asInstanceOf[Self]
  }

  /** Unboxed newtype for Shorts. */
  abstract class short extends Newtype[Short] {
    @inline final def apply(rep: Repr): Self = rep.asInstanceOf[Self]
  }

  /** Unboxed newtype for Ints. */
  abstract class int extends Newtype[Int] {
    @inline final def apply(rep: Repr): Self = rep.asInstanceOf[Self]
  }

  /** Unboxed newtype for Longs. */
  abstract class long extends Newtype[Long] {
    @inline final def apply(rep: Repr): Self = rep.asInstanceOf[Self]
  }

  /** Unboxed newtype for Floats. */
  abstract class float extends Newtype[Float] {
    @inline final def apply(rep: Repr): Self = rep.asInstanceOf[Self]
  }

  /** Unboxed newtype for Doubles. */
  abstract class double extends Newtype[Double] {
    @inline final def apply(rep: Repr): Self = rep.asInstanceOf[Self]
  }

  /** Unboxed newtype for Chars. */
  abstract class char extends Newtype[Char] {
    @inline final def apply(rep: Repr): Self = rep.asInstanceOf[Self]
  }
}

