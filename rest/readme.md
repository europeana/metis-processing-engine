# Rest application

This application is responsible for submitting Metis task on Kubernetes cluster.



### Required permissions needed for service account

described by the following yaml:

````
kind: Role
apiVersion: rbac.authorization.k8s.io/v1
metadata:
name: rest-client-role
rules:
- verbs:
    - create
    - delete
  apiGroups:
    - ''
resources:
    - secrets
    - services
- verbs:
    - create
    - get
    - delete
  apiGroups:
    - batch
resources:
    - jobs
- verbs:
    - create
    - delete
  apiGroups:
    - apps
  resources:
    - deployments
````

