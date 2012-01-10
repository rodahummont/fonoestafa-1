from django.db.models import Manager
from fono.api.models import *

class NumberManager(Manager):

    @staticmethod
    def lookup(number):
        try:
            # put more logic here
            return Number.objects.get(number = number, status = True, denounce__user__status = 1)
        except Number.DoesNotExist:
            return None

    @staticmethod
    def updates():
        return Number.objects.filter(status = True)
