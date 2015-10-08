#!/bin/sh

class_path=build/classes/main
base_dir=../../..
profiles_path=$base_dir/profiles
output_path=$base_dir/../src/main/assets

cd $class_path
java com.ip2o.profilemaker.Main $profiles_path $output_path $base_dir
