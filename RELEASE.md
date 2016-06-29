Make a new version:

    mvn org.eclipse.tycho:tycho-versions-plugin:0.25.0:set-version -DnewVersion=1.0.3-SNAPSHOT
    
Build:

    mvn package
    
Deploy:

    cd path/to/gh-pages/p2
    rm -rf .
    cp repository/target/repository path/to/gh-pages/p2
    git add .
    git rm # remove deleted files from git status
    git commit
    git push