cd "examples/"
# javac Add.java
# java Add
echo "" > "../results.txt"

for fileName in *.java ; do
    eval "javac $fileName"

    IFS="." tokens=($fileName)
    onlyFileName=${tokens[0]}
    outFileName="$onlyFileName-output.txt"
    eval "java $onlyFileName > $outFileName"

    clang-4.0 -o "../results/$onlyFileName-exec" "../results/$onlyFileName.ll"
    eval "../results/$onlyFileName-exec > ../results/$outFileName"

    eval "diff $outFileName ../results/$outFileName >> ../results.txt"

    echo "Output of file $fileName and its llvm-IR equivalent checked" >> "../results.txt"
    echo
done