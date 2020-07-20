(defproject planning-demos "0.0.1-SNAPSHOT"
  :dependencies
  [[org.clojure/clojure "1.10.1"]
   [org.clojure/clojurescript "1.10.773"]
   [thheller/shadow-cljs "2.10.15"]
   [reagent "1.0.0-alpha2"]
   [markbastian/planning "0.1.0-SNAPSHOT"]]

  :plugins [[hiccup-bridge "1.0.1"]]

  :source-paths
  ["src/main"])
