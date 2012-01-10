from django.http import HttpResponse
from django.shortcuts import render_to_response, get_object_or_404
from django.core.urlresolvers import reverse
from fono.api.models import *
from fono.api.managers import *
from fono.api.forms import *
import random

def denounce(request):

    form = DenounceForm(request.POST)
    form = DenounceForm(request.GET)
    if form.is_valid():
        number = form.cleaned_data['number']
        comments = form.cleaned_data['comments']
        number, is_new = Number.objects.get_or_create( number = number, defaults={'status': 1})
        denounce = Denounce.objects.create(number = number, comments = comments)
        return HttpResponse('ok')
    else:
        return HttpResponse('error')

def lookup(request):

    form = LookupForm(request.GET)
    if form.is_valid():
        number = NumberManager.lookup(form.cleaned_data['number'])
        if number:
            return HttpResponse('1;%s' % number.get_date())
        return HttpResponse('0')
    else:
        return HttpResponse('invalid')

def status(request):
    return HttpResponse('with data')

def updates(request):
    numbers = NumberManager.updates()
    response = ['%s;%s' % (number.number, number.get_date()) for number in numbers]
    return HttpResponse('\n'.join(response), content_type='text/plain')

def home(request):
    return render_to_response('base.html')

def register(request):
    form = UserForm(request.POST)
    form = UserForm(request.GET)
    if form.is_valid():
        number = form.cleaned_data['number']
        the_hash = str(random.random())[2:]
        user, is_new = User.objects.get_or_create( number = number, defaults={'the_hash': the_hash})
        # @todo: encode decently
        encoded_number = '%2B' + user.number[1:]
        url = reverse('confirm') + '?number=%s&the_hash=%s' % (encoded_number, user.the_hash)
        # @todo: send a sms or email here with the url
        return HttpResponse('ok;%s' % url)
    else:
        return HttpResponse('error')

def confirm(request):

    form = ConfirmForm(request.POST)
    form = ConfirmForm(request.GET)
    if form.is_valid():
        user = get_object_or_404(User, the_hash = form.cleaned_data['the_hash'], number = form.cleaned_data['number'])
        user.status = 1
        user.save()
        return HttpResponse('ok')
    else:
        return HttpResponse('invalid')
