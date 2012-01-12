from django.http import HttpResponse
from django.shortcuts import render_to_response, get_object_or_404
from django.core.urlresolvers import reverse
from fono.api.models import *
from fono.api.managers import *
from fono.api.forms import *
import random
import urllib

def denounce(request):

    form = DenounceForm(request.POST)
    if form.is_valid():
        number = form.cleaned_data['number']
        comments = form.cleaned_data['comments']
        the_hash = form.cleaned_data['the_hash']
        user = get_object_or_404(User, the_hash = the_hash)
        number, is_new = Number.objects.get_or_create( number = number, defaults={'status': 1})
        denounce = Denounce.objects.create(number = number, comments = comments, user = user)
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
    if form.is_valid():
        number = form.cleaned_data['number']
        the_hash = str(random.random())[2:]
        user, is_new = User.objects.get_or_create( number = number, defaults={'the_hash': the_hash})
        query = '?' + urllib.urlencode({'number': user.number, 'the_hash': user.the_hash})
        url = reverse('confirm') + query
        # @todo: send a sms or email here with the url
        # @todo: dont show the hash here!
        return HttpResponse('ok;%s' % url)
    else:
        return HttpResponse('error')

def confirm(request):

    form = ConfirmForm(request.GET)
    if form.is_valid():
        user = get_object_or_404(User, the_hash = form.cleaned_data['the_hash'], number = form.cleaned_data['number'])
        user.status = 1
        user.save()
        response = { 'message': 'N&uacute;mero registrado exitosamente' }
        return render_to_response('confirm.html', response)
    else:
        response = { 'message': 'Oops, no pudimos completar tu registro.' }
        return render_to_response('confirm.html', response)
