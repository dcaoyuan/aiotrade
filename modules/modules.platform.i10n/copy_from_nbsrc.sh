#!/bin/sh

if [ $# -lt 1 ]
then
    echo "Need parameter <src_dir>"
    exit 1
fi

src_dir=$1/platform
dest_dir=./src/main/resources/

echo "src_dir is ${src_dir}"

for folder in ${src_dir}/*;
do
    if [ -d ${folder} ]
    then
        for module_name in ${folder}/*;
        do
            module_base=`basename ${module_name}`
            echo "dir: ${module_base}"
            for module_name1 in ${folder}/${module_base}/*;
            do
                module_base1=`basename ${module_name1}`
                for module_src in ${folder}/${module_base}/${module_base1}/*;
                do
                    module_base2=`dirname ${module_src}`
                    echo "coping ${module_base2} to ${dest_dir}"
                    cp -r ${module_base2} ${dest_dir}
                done
            done
        done
    fi
done
