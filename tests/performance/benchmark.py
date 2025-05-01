import pytest
import asyncio
import aiohttp
from typing import Dict, Any

@pytest.fixture
async def http_client():
    async with aiohttp.ClientSession() as session:
        yield session

@pytest.mark.asyncio
@pytest.mark.benchmark
async def test_node_status_response_time(benchmark, http_client):
    async def _get_status():
        async with http_client.get("http://localhost:8080/api/v1/nodes/status") as response:
            return await response.json()
    
    result = await benchmark(_get_status)
    assert isinstance(result, dict)

@pytest.mark.asyncio
@pytest.mark.benchmark
async def test_task_submission(benchmark, http_client):
    async def _submit_task():
        task_data = {
            "type": "test_task",
            "payload": {"test": "data"}
        }
        async with http_client.post("http://localhost:8080/api/v1/tasks", json=task_data) as response:
            return await response.json()
    
    result = await benchmark(_submit_task)
    assert isinstance(result, dict)

@pytest.mark.asyncio
@pytest.mark.benchmark
async def test_cluster_metrics(benchmark, http_client):
    async def _get_metrics():
        async with http_client.get("http://localhost:8080/api/v1/cluster/metrics") as response:
            return await response.json()
    
    result = await benchmark(_get_metrics)
    assert isinstance(result, dict) 