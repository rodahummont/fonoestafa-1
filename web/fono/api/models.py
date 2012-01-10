from django.db import models

class Number(models.Model):
    number = models.CharField(max_length=100, blank=False)
    status = models.SmallIntegerField(blank=True, default=0)
    # change for choicefield
    # type = models.SmallIntegerField(blank=True, default=0)
    created_at = models.DateTimeField(editable=False,auto_now_add=True)

    def __unicode__(self):
        return  '%d - %s' % (self.id, self.number)

    def get_date(self):
        return str(self.created_at).split()[0]

class Denounce(models.Model):
    user = models.ForeignKey('User', blank=False) # denounced_by
    number = models.ForeignKey('Number', blank=False)
    comments = models.TextField(blank=True)
    created_at = models.DateTimeField(editable=False,auto_now_add=True)

    def __unicode__(self):
        return  '%d - %s' % (self.id, self.number)

class User(models.Model):
    number = models.CharField(max_length=100, blank=False)
    status = models.SmallIntegerField(blank=True, default=0)
    created_at = models.DateTimeField(editable=False,auto_now_add=True)
    the_hash = models.CharField(max_length=100, unique = True)

    def __unicode__(self):
        return  '%d - %s' % (self.id, self.number)
