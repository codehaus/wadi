#!/bin/sh

ssh codehaus.org "sh -c 'cd ~wadi; rm -fr public_html.orig;mv public_html public_html.orig; mv public_html.orig/stage public_html'"
