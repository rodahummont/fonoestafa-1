#!/usr/bin/env python

# url: http://www.gestalts.net/Create_an_HTTPS_server_in_Python
#
# Generate private key
#	openssl genrsa -out 10.0.2.2.key 2048
#
# Generate CSR (certificate signing request)
#	openssl req -new -key 10.0.2.2.key -out 10.0.2.2.csr
#	ON, OU, CN deben ser 10.0.2.2
#
# Remove Passphrase
#	cp 10.0.2.2.key 10.0.2.2.key.orig
#	openssl rsa -in 10.0.2.2.key.orig -out 10.0.2.2.key
#
# Generate Self-Signed Certificate
#	openssl x509 -req -days 999 -in 10.0.2.2.csr -signkey 10.0.2.2.key -out 10.0.2.2.crt
#
# Create the PEM
#	cat 10.0.2.2.{key,crt} > 10.0.2.2.pem
#

from urlparse import urlparse
import time, sys, ssl, SocketServer 
from BaseHTTPServer import HTTPServer
from SimpleHTTPServer import SimpleHTTPRequestHandler
import ssl


PORT = 8000
CERTFILE = '10.0.2.2.pem'

LAST_DAY_NUMBER = 1
LAST_PHN_NUMBER = 996601

class RodHTTPHandler(SimpleHTTPRequestHandler):
	def make_dates(self):
		global LAST_DAY_NUMBER, LAST_PHN_NUMBER
		d1 = '%d;2011-12-%02d 10:20:30' % ((LAST_PHN_NUMBER + 0), (LAST_DAY_NUMBER + 0))
		d2 = '%d;2011-12-%02d 10:20:30' % ((LAST_PHN_NUMBER + 1), (LAST_DAY_NUMBER + 1))
		d3 = '%d;2011-12-%02d 10:20:30' % ((LAST_PHN_NUMBER + 2), (LAST_DAY_NUMBER + 2))
		LAST_DAY_NUMBER += 3
		LAST_PHN_NUMBER += 3
		return [d1, d2, d3]

	
	def do_GET(self):
		print 'do_GET'
		args = urlparse(self.path)
		if args.path == '/lookup':
			query = args.query.split('&')
			for param in query:
				key, val = param.split('=', 1)
				if (key == 'number'):
					print 'consultan por', val
					num = int(val)
					if ((num % 2) != 0):
						print num,'es numero denunciado!!!'
						return self.response(msg=['si;2012-01-07'] + self.make_dates())
					else:
						return self.response(msg='no')
				else:
					print key, ' --> ', val

		elif args.path == '/denounce':
			print 'denuncia!!!'
			query = args.query.split(',')
			for param in query:
				print ' ', param
			return self.response()

		elif args.path == '/updates':
			print 'updates!!!'
			query = args.query.split(',')
			for param in query:
				print ' ', param
			return self.response(msg=self.make_dates())

		elif args.path == '/status':
			print 'status!!!'
			if LAST_DAY_NUMBER == 1:
				msg = 'EMPTY'
			else:
				msg = 'WITH DATA'
			print msg
			return self.response(msg=msg)

		else:
			return SimpleHTTPRequestHandler.do_GET(self)


	def send401(self):
		H = {
			'WWW-Authenticate': 'Digest realm="fonoestafa", qop="auth,auth-int", nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093", opaque="5ccc069c403ebaf9f0171e9517f40e41"'
		}
		return self.response(msg='', code=401, headers=H)


	def is_authorized(self):
		s = self.headers.get('Authorization', '')
		if s == '' or s[:7] != 'Digest ':
			return False
		return True

	def do_POST(self):
		print 'do_POST'
		args = urlparse(self.path)
		if args.path == '/create':
			return self.response(msg='OK')
		elif args.path == '/register':
			print 'register...'
			print 'headers:', self.headers
			return self.response(msg='OK;dabalearrozalazorraelabad', code=200) 
		elif args.path == '/register_confirm':
			print 'register_confirm...'
			print 'headers:', self.headers
			if not self.is_authorized():
				print 'no autorizao'
				self.send401()
			else:
				print '----------ok'
				return self.response(msg='OK')
		elif args.path == '/denounce':
			print 'denuncia!!!'
			if not self.is_authorized():
				print 'no autorizao'
				self.send401()
			else:
				print '----------ok'
				query = args.query.split(',')
				for param in query:
					print ' ', param
				return self.response()
		else:
			return self.response(code=404)

		

	def response(self, msg='', code=200, headers={'Content-type': 'text/plain'}):
		self.send_response(code)
		for key, val in headers.items():
			self.send_header(key, val)
		self.end_headers()
		if type(msg) == list:
			msg = '\n'.join(msg)
		self.wfile.write(msg)


if (len(sys.argv) > 1) and (sys.argv[1] == 'https'):
	print 'usando https'
	httpd = HTTPServer(('', PORT), RodHTTPHandler)
	httpd.socket = ssl.wrap_socket(httpd.socket, certfile=CERTFILE, server_side=True)
else:
	print 'servidor en claro'
	Handler = RodHTTPHandler
	httpd = SocketServer.TCPServer( ("", PORT), Handler )


try:
	httpd.serve_forever()
except KeyboardInterrupt:
	print 'fin'
