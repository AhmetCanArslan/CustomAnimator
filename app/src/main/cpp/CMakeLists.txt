cmake_minimum_required(VERSION 3.22.1...4.1)
project(evdev C)

add_library(evdev SHARED evdev.c)
target_compile_options(evdev PRIVATE -Wall -Wextra -Wno-unused-parameter -O2)
