#!/bin/bash
shell_path=$(dirname $(readlink -f $0))
apks=( $(find $shell_path -name "*.apk") )
for((i=0;i<${#apks[@]};++i))
do
	echo "$i ${apks[$i]}"
done
echo -n "chose apk to upload: "
read i
apk=${apks[$i]}
echo $apk
echo "1 pgyer"
echo "2 fir"
echo -n "chose platform to upload: "
read platform
if [ $platform == 1 ]
then
    curl https://www.pgyer.com/apiv2/app/upload \
    -F 'buildUpdateDescription=' \
    -F "file=@$apk" \
    -F '_api_key=92e6efea0733406def858f387f2e5ccd'
elif [ $platform == 2 ]
then
    ruby /home/wtuadn/.gem/ruby/2.5.0/gems/fir-cli-1.6.10/bin/fir p $apk -V -T 43d12f8076eb1ec205e6b90c42db0df6
fi