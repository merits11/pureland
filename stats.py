#!/usr/local/bin/python

import argparse
from collections import defaultdict
from datetime import datetime, timedelta

import boto3
from dateutil import parser


arg_parser = argparse.ArgumentParser(description='Stats for pure land skill')
arg_parser.add_argument('-d', '--days', metavar='DAYS', type=int, default=30,
                        help='Number of recent days to count')
args = arg_parser.parse_args()

TOKEN = u'token'
LANG = u'language'
LIST = u'currentList'
USER = u'userid'
DEVICE = u'deviceId'
LAST_VERSION = u'lastHeardVersion'
LAST_MODIFIED = u'lastModified'


def full_scan():
    # Get the service resource.
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table('pureLandTable-Beta')
    response = table.scan()
    data = response['Items']
    while 'LastEvaluatedKey' in response:
        response = table.scan(ExclusiveStartKey=response['LastEvaluatedKey'])
        data.extend(response['Items'])
    return data


def stats(days):
    items = full_scan()
    now_time = datetime.utcnow()
    users = defaultdict(int)
    stats = defaultdict(int)
    playlists = defaultdict(set)
    devices = set()
    recentUsers = set()
    total = 0
    for item in items:
        token = item[TOKEN]
        if token.startswith(u'amzn1'):
            total += 1
            if item[LANG] == u'English':
                users['English'] += 1
            elif item[LANG] == u'Chinese':
                users['Chinese'] += 1
            else:
                users['All Languages'] += 1
            if LAST_VERSION in item:
                stats[item[LAST_VERSION]] += 1
            else:
                stats['Legacy version'] += 1
        else:
            lastUpdated = parser.parse(item[LAST_MODIFIED])
            lastUpdatedNaive = lastUpdated.replace(tzinfo=None)
            if (now_time - lastUpdatedNaive) >= timedelta(days=days):
                continue
            userId = item[USER]
            deviceId = item[DEVICE]
            devices.add(deviceId)
            recentUsers.add(userId)
            playlists[item[LIST]].add(userId)

    print "Users [All Time]:"
    print " - Total items: %d" % len(items)
    print " - Total Users: %d" % total
    for k, v in users.iteritems():
        print '    %s: %d' % (k, v)
    print " - Version breakdown:"
    for k, v in stats.iteritems():
        print "    %s: %d" % (k, v)

    print

    print "Recent stats [%d day(s) ago - now]:" % days
    print " - Total devices : %d" % len(devices)
    print " - Total users : %d" % len(recentUsers)
    print " - Play list rank:"
    for k, v in sorted(playlists.iteritems(), key=lambda (k1, v1): (len(v1), k1), reverse=True):
        print "    %s: %d users" % (k, len(v))


stats(args.days)
