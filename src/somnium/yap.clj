(ns somnium.yap
  (:use [clojure.contrib.core :only [seqable?]]
	[clojure.contrib.pprint :only [pprint]]))

(defn raise! [& msgs]
  (throw (Exception. (apply str msgs))))

(defn pps [x] (with-out-str (pprint x)))

(defmacro when* [expr more]
  `(if ~expr ~more ::match-failure))

(defn quote? [x] 
  (and (seq? x) (= 'quote (first x))))

(defn wildcard? [x] (= '_ x))

(defn sentinel? [x] (= '& x))

(defn fold* ([x] x) ([x & [y & ys]] (apply fold* (y x) ys)))

(defn fold [xs] (apply fold* xs))

(declare match-vec match-map match-seq)

(defn match-one [val pat]
  (condp (fn [f x] (f x)) pat
    vector?   (match-vec val pat)
    map?      (match-map val pat)
    seq?      (match-seq val pat)
    wildcard? identity
    symbol?   (fn [more] `(let [~pat ~val] ~more))
    quote?    (fn [more] `(when* (= ~val ~(first pat)) ~more))
              (fn [more] `(when* (= ~val ~pat) ~more))))

(defn match-vec [val pat]
  (let [[xs [_ y]] (split-with (comp not sentinel?) pat)
	xs   (vec xs)
	xlen (count xs)
	sym  (gensym "mvec")
	gets (for [[_ i] (map vector xs (iterate inc 0))] 
	       `(nth ~sym ~i))
	gets (if-not y 
	       gets
	       (-> gets vec (conj `(nthnext ~sym ~xlen))))
	xs   (if-not y xs (conj xs y))
	op   (if y '>= =)]
    (fn [more]
      `(when* 
	(seqable? ~val)
	(let [~sym ~val]
	  (when* 
	   (~op (count ~sym) (count (quote ~xs)))
	   ~(fold (cons more
			(reverse (map match-one gets xs))))))))))

(defn match-map [val pat]
  (let [mmap (gensym "mmap")
	fns  (for [[k v] pat]
	       (let [s (gensym)]
		 (fn [more] 
		   `(let [~s (get ~mmap ~k ::match-failure)]
		      (when* ~s ~((match-one s v) more))))))]
    (fn [more]
      `(when*
	(instance? java.util.Map ~val)
	(let [~mmap ~val]
	  ~(fold (cons more fns)))))))

(defn match* [v [a b & more :as clauses] doc]
  (if-not (seq clauses)
    `(raise! "no matching clause for: " (pr-str ~v)
	      " among patterns:\n" 
	      ~@(interpose "\n" (map (fn [x] `(quote ~x)) doc)))
     `(let [t# ~((match-one v a) b)]
	(if-not (= t# ::match-failure) t#
		~(match* v more doc)))))

(defmacro match 
  {:arglists '([expr & pat-exprs])}
  [& [a & bs]]
  (let [doc (vec (for [[x _] (partition 2 bs)] x))]
    (match* a bs doc)))

(defmacro fm [& pats]
  `(fn [& s#] (match s# ~@pats)))

(defmacro defm [name & pats]
  `(defn ~name [& xs#]
     (match xs# ~@pats)))

(defn match-seq [val [op arg as]]
  (fn [more]
    (let [f (fn [p] `(when* ~p ~(if as `(let [~as ~val] ~more) more)))]
      (match op
        :?    (f `(~arg ~val))
	:type (f `(= ~arg (type ~val)))))))