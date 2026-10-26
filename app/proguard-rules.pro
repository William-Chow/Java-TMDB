# Project-specific R8 rules. The defaults come from proguard-android-optimize.txt (see build.gradle).
#
# Nothing needs keeping by hand: the libraries that rely on reflection ship their own rules.
#  - Gson 2.13 keeps every class with @SerializedName fields, those fields and the no-arg
#    constructor, plus generic signatures. Every model in com.movie.tmdb.app.model maps its
#    fields with @SerializedName, so new fields must too.
#  - Retrofit 3 keeps interfaces whose methods carry its HTTP annotations (APIInterface).
#  - Room generates code that references the entities and DAOs directly.
# Package-wide "-keep class ... { *; }" rules used to sit here; they stopped R8 from shrinking
# or optimizing those packages at all.
