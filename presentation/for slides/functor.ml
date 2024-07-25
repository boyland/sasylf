module type Input = sig
  type e
  type f
  type g = 
    | E of e
    | F of f
  type plus = e * e * e
  type times = g * g * g
  type both =
    | BothRule of plus * times
    | PlusRule of plus

  type plus_commutes = plus -> plus
  val plus_commutes_proof : plus_commutes
end

module Functor = functor(M: Input) -> struct
(* Body *)
end

