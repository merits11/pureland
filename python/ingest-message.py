import boto3
import argparse



def main(args):
    cut_off_date = None



if __name__ == u"__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument(u'--update', default=False, action=u'store_true', help=u'Update it or not.')
    parser.add_argument(u"-r", u'--recursive', default=False, action=u'store_true', help=u'Recursive update')
    parser.add_argument(u"-s", u'--show', default=False, action=u'store_true', help=u'Print files only')
    parser.add_argument(u'--since', type=float, default=-1, help=u'Work only on files days ago')
    parser.add_argument(u'-d', u'--dir', required=True, help=u'Directory to find mp3 files')
    parser.add_argument(u'-t', u'--target', default=-17, type=float, help=u'Target DB')
    parser.add_argument(u'-m', u'--minChange', default=3, type=float, help=u'Min db to add')
    args = parser.parse_args()
    main(args)