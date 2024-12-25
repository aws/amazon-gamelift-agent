CHANGELOG

# 1.0.3 (12/30/2024)
- Add termination reason to NotifyServerProcessTermination message
- Enable compute registration by default when running on non-container compute
- Minor bug fixes

# 1.0.2 (11/04/2024)
- Update compute registration for container fleets
- Skip spot termination checks when running on non-spot or non-EC2 instances
- Add option to use AWS default credential provider chain to upload logs
- Remove DeregisterCompute call for container compute when termination is started
- Minor bug fixes

# 1.0.1 (07/23/2024)
- Add support for UNKNOWN_WINDOWS and UNKNOWN_LINUX platforms
- Add initial GitHub Action to compile/test Agent with Maven automatically
- Minor bug fixes

# 1.0.0 (04/24/2024)
- Initial public release of amazon-gamelift-agent