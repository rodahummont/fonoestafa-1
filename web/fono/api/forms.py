from django import forms
from fono.api.models import User

# with chilean cellphone regex validator

class DenounceForm(forms.Form):
    number = forms.RegexField(max_length=12, required=True, regex='^\+[0-9]{11}$')
    the_hash = forms.CharField(max_length=100, required=True)
    comments = forms.CharField(required=False)

class LookupForm(forms.Form):
    number = forms.RegexField(max_length=12, required=True, regex='^\+[0-9]{11}$')
    # include_updates = forms.BooleanField(required=False)

class UserForm(forms.Form):
    number = forms.RegexField(max_length=12, required=True, regex='^\+[0-9]{11}$')

class ConfirmForm(forms.Form):
    number = forms.RegexField(max_length=12, required=True, regex='^\+[0-9]{11}$')
    the_hash = forms.CharField(max_length=100, required=True)