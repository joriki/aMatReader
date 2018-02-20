

from setuptools import setup, find_packages
from os import path

here = path.abspath(path.dirname(__file__))

setup(
    name='pyCyAMatReader',
    version='0.0.1',
    description='Python interface for the AMatReader Cytoscape app',
    #url='',
    author='Brett Settle',
    #author_email='',
    license='MIT',
    classifiers=[
        # How mature is this project? Common values are
        #   3 - Alpha
        #   4 - Beta
        #   5 - Production/Stable
        'Development Status :: 3 - Alpha',
        'License :: OSI Approved :: MIT License',
        'Programming Language :: Python :: 3.4',
        'Programming Language :: Python :: 3.5',
    ],
    keywords='Cytoscape',
	packages=find_packages(),
    #install_requires=['peppercorn'],

    # To provide executable scripts, use entry points in preference to the
    # "scripts" keyword. Entry points provide cross-platform support and allow
    # pip to create the appropriate form of executable for the target platform.
    #entry_points={
    #    'console_scripts': [
    #        'newmodule=newmodule:main'
    #        #'sample=sample:main',
    #    ],
    #},
)
