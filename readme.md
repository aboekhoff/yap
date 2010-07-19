# yap

Yet Another Pattern-Matcher    

A short-circuting matcher that performs its own
destructuring and manages to comes in under 100 lines.

## Usage

the-basic form is (match x pat expr ...)

## Literals

Literals (strings, keywords, numbers and quoted forms)    
match with =

    (match "foo"
      "foo" :foo) => :foo

    (match 'foo
      'foo :foo) => :foo

    (match '(some list of symbols)
      '(some list of symbols) :foo) => :foo

## Symbols

Symbols bind variables as in destructuring

    (match :foo 
      x x) => :foo

Except for _, which matches anything and ignores it.

    (match :something
      _ :foo) => :foo

## Vectors

Pretty much the same as clojure's vector destructuring --     
they match against anything seqable

    (match [1 2 3 4]
      [_ a _ b] (+ a (* b 10))) => 42

    (match [1 2 3 4]
      [_ a b :foo] :wtf
      [_ a b _]    (+ a (* b 10))) => 42

including strings    

    (match "foo"
      [_ _ \o] :foo) => :foo

and nil    

    (match nil
      [] :nothingness) => :nothingness

and the symbol `&' indicates match any more as usual

    (match (range 1 5)
      [a b c]      (* a b c)
      [a b c & ds] ds)
        => (4 5)

but size does matter    

    (match [1 2 3 4]
      [_ _]       :wtf
      [_ _ _]     :wtf
      [_ _ _ & _] :yup) => :yup

## Maps

Maps match against any instance of java.util.Map    
Their syntax is a bit different from clojure's
destructuring.

    (match {:frodo "gandalf" :frobozz "hello sailor"}
      {:frodo "hobbit"}  :a
      {:frodo "gandalf" 
       :peppermint x}    :b
      {:frodo "gandalf"
       :frobozz x}       x) => "hello sailor"

In short, the key (it doesn't have to be a keyword)    
comes on the left, and the pattern on the right. 

All of the listed keys in the pattern must match for the match     
to be successful, but not all of the keys in the map need be used.

## Lists

List patterns are treated as special forms.

A list beginning with `?' introduces a function whose result   
matches if it is truthy.

    (match :foo
      (? keyword?) :yep) => :yep

A list beginning with `type' is just a little sugar for
  
    `(= (type ~pat-expr) ~match-variable)

When a list has a symbol as its last element it binds a    
successful match to it.    

    (match [:acme :quendor :avatar]
      [_ "foo" _] :wtf
      [_ (? keyword? x) (? string? y)] :wtf    
      [_ (? keyword? x) (? keyword? y)] [x y]) => [:quendor :avatar]    

so    

    (match 'foo
       (type clojure.lang.Symbol) :yep) => :yep

## Nesting

Patterns nest arbitrarily.    
They short circuit from left-to-right and top-to-bottom.

## Errors

Unsucessful matches raise an exception with the input and the patterns

    (match :glaring-logical-error
      [42 42 42] :declare-world-peace
      "foobar"   :fire-the-missiles)

    => no matching pattern for :glaring-logical-error among    
       [42 42 42]
       "foobar"
       thrown java.lang.Exception ...

## Functions

Currently there are two tiny macros:

fm   ([& pats]) => `(fn [& xs#] (match xs# ~@pats))       

defm ([name & pats]) => `(defn ~name [& xs#] (match xs# ~@pats))    

## TODO

Tests    
Docstrings    
Support regex-patterns?    
Sugar for guards?    
Make list patterns more flexible/extensible?    

## License

Eclipse
