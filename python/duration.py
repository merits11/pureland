#!/usr/local/bin/python3
import codecs
import os
from glob import glob

import mutagen


# /Users/junmao/Dropbox/Code/AlexaSkillsSet/pureland/duration.py

def walk_dir(folder, pf):
    good, another_option, bad = 0, 0, 0
    for audio_file in glob(folder + u"/*.*"):
        if not audio_file.endswith(u".mp3") and not audio_file.endswith(u".m4a"):
            continue
        try:
            audio = mutagen.File(audio_file)
            pf.write(u"%s = %d\n" % (audio_file, audio.info.length * 1000))
            good += 1
        except Exception as e1:
            print(u"\tFailed to process {0}: {1}".format(audio_file, e1))
            bad += 1
    return (good, bad)


with codecs.open(u'duration.properties', encoding='utf-8', mode='w+') as pf:
    total_good, total_bad = 0, 0
    for folder in glob(u"*"):
        if not os.path.isdir(folder):
            continue
        good, bad = walk_dir(folder, pf)
        total_good += good
        total_bad += bad
    print(u"Total processed %d, failed %s" % (total_good, total_bad))
