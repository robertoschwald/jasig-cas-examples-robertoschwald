DirectMappedPersonAttributeDao
------------------------------
By default, the PersonAttributeDao implementations of the Jasig Person-Directory library need an extra request
after sucessful authentication to pull user attributes to provide them to CAS Client applications.
The DirectMappedPersonAttributeDao is a short-term caching attributeRepository, which can be filled with user attributes
from beans directly (e.g. by AuthenticationHandlers).
