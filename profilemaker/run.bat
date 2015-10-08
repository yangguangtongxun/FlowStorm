@echo off

set class_path=build/classes/main
set base_dir=../../..
set profiles_path=%base_dir%/profiles
set output_path=%base_dir%/../app/src/main/assets

cd %class_path%
java com.ip2o.profilemaker.Main %profiles_path% %output_path% %base_dir%
pause
