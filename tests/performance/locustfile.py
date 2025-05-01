from locust import HttpUser, task, between

class MeshNodeUser(HttpUser):
    wait_time = between(1, 5)
    
    @task(3)
    def get_node_status(self):
        self.client.get("/api/v1/nodes/status")
    
    @task(2)
    def get_cluster_metrics(self):
        self.client.get("/api/v1/cluster/metrics")
    
    @task(1)
    def submit_task(self):
        self.client.post("/api/v1/tasks", json={
            "type": "test_task",
            "payload": {"test": "data"}
        }) 