<script>
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { get } from 'svelte/store';
  import { isLoggedIn } from '$lib/stores.js';

  onMount(() => {
    goto(get(isLoggedIn) ? '/tasks' : '/login', { replaceState: true });
  });
</script>
