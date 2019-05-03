import numpy as np

"""
neurons
"""
class AbstractNeuron():
	""" Abstract class for a neuron

	Attributes
	----------
	I : float (Default: 0)
		Current membrane voltage of the neuron
	out : float (Default: 0)
		Current neuron output
	"""
	I = 0
	out = 0

	def __init__(self, amplitude=1):
		self.amplitude = amplitude

	def update_rng(self, rng):
		if hasattr(self, 'rng'):
			self.rng = rng


class LIF(AbstractNeuron):
	"""Leaky integrate-and-fire based on the Sandia model

	Parameters
	----------
	m : float
		Leakage constant
	V_init : float
		Initial membrane voltage (Default: 0)
	V_reset : float
		Reset voltage when the neuron has spiked (Default: 0)
	thr : float
		Spiking threshold (Default: 1)
	amplitude : float (Default: 1)
		Amplitude of the output when the neuron spikes
	I_e : float (Default: 0)
		Constant input current
	noise : float (Default: 0)
		Standard deviation of the normal distribution that is sampled from 
		to add noise to the membrane voltage at each step
	rng : np.random.RandomState
		Random generator for the noise
	"""

	def __init__(self, m, V_init=0, V_reset=0, V_rest=0, thr=1, amplitude=1, I_e=0, noise=0, rng=None):
		AbstractNeuron.__init__(self, amplitude)
		self.m = m
		self.V = V_init
		self.V_reset = V_reset
		self.V_rest = V_rest
		self.thr = thr
		self.I_e = I_e
		self.rng = rng if rng != None else np.random.RandomState()
		self.noise = noise
		

	def step(self):
		self.V = self.V * self.m + self.I # update V
		if self.noise > 0:
			self.V += self.rng.normal(scale=self.noise) # add noise
		self.V = max(self.V_rest, self.V)
		self.I = self.I_e # reset I with I_e
		if self.V > self.thr: # check for spike
			self.V = self.V_reset
			self.out = self.amplitude
		else:
			self.out = 0



"""
Generators
"""
class SpikeTrain(AbstractNeuron):
	"""Generator that outputs a given train of events

	Parameters
	----------
	train : array_like
		Output train
	amplitude : float (Default: 1)
		Amplitude of the output
	"""
	def __init__(self, train, amplitude=1):
		AbstractNeuron.__init__(self, amplitude)
		self.train = train
		self.size = len(train)
		self.index = 0

	def step(self):
		self.out = self.train[self.index] * self.amplitude
		self.index = (self.index + 1) % self.size

class PoissonGenerator(AbstractNeuron):
	"""Generator that fires with Poisson statistics, i.e. exponentially 
	distributed interspike intervals.

	Parameters
	----------
	I_e : float (Default: 0)
		Constant input current
	amplitude : float (Default: 1)
		Amplitude of the output
	rng : np.random.RandomState
		Random generator
	"""

	def __init__(self, I_e=0, amplitude=1, rng=None):
		AbstractNeuron.__init__(self, amplitude)
		self.I_e = I_e
		self.rng = rng if rng != None else np.random.RandomState()

	def step(self):
		self.out = 0
		if self.I > 0:
			while (-np.log(1-self.rng.rand()) / self.I) < 1:
				self.out += 1
			
		self.out *= self.amplitude
		self.I = self.I_e

