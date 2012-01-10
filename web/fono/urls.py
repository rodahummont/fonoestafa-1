from django.conf.urls.defaults import *
from fono.api.views import *

urlpatterns = patterns('',
    url(r'^denounce$', denounce, name='denounce'),
    url(r'^lookup$', lookup, name='lookup'),
    (r'^status$', status),
    (r'^updates$', updates),
    (r'^register$', register),
    url(r'^confirm$', confirm, name='confirm'),
    (r'^$', home),
)
